package news;

import akka.Done;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import alpakkeer.core.jobs.JobStreamBuilder;
import alpakkeer.core.scheduler.model.CronExpression;
import alpakkeer.core.util.Operators;
import alpakkeer.core.values.Nothing;
import alpakkeer.javadsl.Alpakkeer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;
import lombok.AllArgsConstructor;
import news.crawler.ARDCrawler;
import news.crawler.Crawler;
import news.crawler.SpiegelCrawler;
import news.model.NewsItem;
import news.model.NewsItemSummary;
import news.ports.NewsItemStorePort;
import org.apache.commons.io.FileUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(staticName = "apply")
public final class CoreApplication {

   private final ApplicationConfiguration config;

   private final NewsItemStorePort store;

   public void run() {
      Alpakkeer.create()
         .withJob(jobs -> jobs
            .create("crawler--ard-teletext")
            .runGraph(b -> createCrawlerGraph(b, "ARD Teletext", ARDCrawler.apply(), config.getSeedsARDTeletext()))
            .withScheduledExecution(CronExpression.everyHours(1))
            .withLoggingMonitor())
         .withJob(jobs -> jobs
            .create("crawler--spiegel-online")
            .runGraph(b -> createCrawlerGraph(b, "Spiegel Online", SpiegelCrawler.apply(), config.getSeedsSpiegelOnline()))
            .withScheduledExecution(CronExpression.everyHours(1))
            .withLoggingMonitor())
         .withJob(jobs -> jobs
            .create("export", cfg -> cfg.withInitialContext(0))
            .runGraph(this::export)
            .withApiEndpoint((app, j) -> app.get(
               "/api/news/export",
               ctx -> {
                  var result = j
                     .start()
                     .thenApply(count -> "Exported " + count + " news items")
                     .toCompletableFuture();

                  ctx.result(result);
               })))
         .withApiEndpoint(app ->
            app
               .get(
                  "/api/news",
                  ctx -> ctx.json(store.findAll().toCompletableFuture()))
               .get(
                  "/api/news/items/:hash",
                  ctx -> {
                     var item = store.findNewsItemByHash(ctx.pathParam("hash"));

                     if (ctx.queryParam("markdown") != null || ctx.queryParam("md") != null) {
                        ctx.res.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
                        ctx.result(
                           item.thenApply(i -> i.map(NewsItem::toMarkdown).orElse("")).toCompletableFuture());
                     } else {
                        ctx.json(item.toCompletableFuture());
                     }
                  }))
         .start();
   }

   private RunnableGraph<CompletionStage<Integer>> export(JobStreamBuilder<Nothing, Integer> b) {
      var exportPath = Path.of(config.exportPath);

      return Source
         .fromCompletionStage(store.findAll())
         .mapConcat(items -> {
            var om = new CsvMapper();
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            om.registerModule(new JavaTimeModule());
            om.registerModule(new Jdk8Module());
            om.registerModule(new DefaultScalaModule());

            var schema = om.schemaFor(NewsItemSummary.class).withHeader();

            Operators.suppressExceptions(() -> {
               FileUtils.deleteDirectory(exportPath.toFile());
               Files.createDirectories(exportPath);
            });

            Operators.suppressExceptions(() -> {
               var file = exportPath.resolve("_items.csv").toFile();
               om.writer().with(schema).writeValues(file).writeAll(items);
            });

            return items;
         })
         .map(NewsItemSummary::getHash)
         .mapAsync(5, store::findNewsItemByHash)
         .toMat(Sink.fold(0, (count, maybeItem) -> {
            if (maybeItem.isPresent()) {
               var item = maybeItem.get();
               var file = exportPath.resolve(item.getHash() + ".content.txt").toFile();

               FileUtils.writeStringToFile(file, item.getText(), StandardCharsets.UTF_8);

               return count + 1;
            } else {
               return count;
            }
         }), Keep.right());
   }

   private RunnableGraph<CompletionStage<Done>> createCrawlerGraph(JobStreamBuilder<Nothing, Done> b, String name, Crawler crawler, List<String> seeds) {
      return Source
         .from(seeds)
         .map(url -> {
            b.getLogger().info("Fetching news items from `" + url + "`");
            return url;
         })
         .mapAsync(1, crawler::getNewsItems)
         .mapConcat(urls -> urls)
         .map(url -> {
            b.getLogger().info("Extracting news item from `" + url + "`");
            return url;
         })
         .mapAsync(5, crawler::extractNewsItem)
         .filter(item -> {
            var title = item.getTitle() != null && item.getTitle().length() > 0;
            var text = item.getText() != null && item.getText().length() > 0;

            return title && text;
         })
         .mapAsync(1, store::insertOrUpdateNewsItem)
         .toMat(Sink.fold(0, (count, item) -> count + 1), Keep.right())
         .mapMaterializedValue(countCS -> countCS.thenApply(count -> {
            b.getLogger().info("Fetched " + count + " news item from " + name);
            return Done.getInstance();
         }));
   }

}
