package news.crawler;

import alpakkeer.core.util.Operators;
import com.google.common.hash.Hashing;
import lombok.AllArgsConstructor;
import news.model.ARDNewsItem;
import news.model.NewsItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor(staticName = "apply")
public final class ARDCrawler implements Crawler {

   public CompletionStage<Set<String>> getNewsItems(String clusterUrl) {
      return CompletableFuture.supplyAsync(() -> Operators.ignoreExceptionsWithDefault(
         () -> {
            Document doc = Jsoup.connect(clusterUrl).get();

            return doc
               .select("div.cluster a")
               .stream()
               .map(e -> e.attr("abs:href"))
               .collect(Collectors.toSet());
         },
         Set.of()));
   }

   public CompletionStage<NewsItem> extractNewsItem(String newsItemUrl) {
      return CompletableFuture.supplyAsync(() -> Operators.suppressExceptions(
         () -> {
            Document doc = Jsoup.connect(newsItemUrl).get();

            var title = doc.select("h2").text();

            var text = doc
               .select("div.std p")
               .stream()
               .map(Element::text)
               .collect(Collectors.joining("\n\n"));

            var hash = Hashing
               .sha256()
               .newHasher()
               .putString(title, doc.charset())
               .putString(text, doc.charset())
               .hash()
               .toString();

            return ARDNewsItem.apply(newsItemUrl, title, text, hash, Instant.now());
         }
      ));
   }

}
