package news.crawler;

import alpakkeer.core.util.Operators;
import news.model.NewsItem;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class ClusterCrawlerTest {

   @Test
   public void testARD() throws ExecutionException, InterruptedException {
      var crawler = ARDCrawler.apply();

      var result = crawler
         .getNewsItems("https://www.ard-text.de/mobil/101")
         .toCompletableFuture()
         .get();

      System.out.println("---");

      result
         .stream()
         .findFirst()
         .map(crawler::extractNewsItem)
         .map(cs -> Operators.suppressExceptions(() -> cs.toCompletableFuture().get()))
         .ifPresent(System.out::println);
   }

   @Test
   public void testSpiegel() throws ExecutionException, InterruptedException {
      var crawler = SpiegelCrawler.apply();

      crawler
         .getNewsItems("https://www.spiegel.de/schlagzeilen/index.rss")
         .toCompletableFuture()
         .get()
         .stream()
         .findFirst()
         .map(crawler::extractNewsItem)
         .map(cs -> Operators.suppressExceptions(() -> cs.toCompletableFuture().get()))
         .map(NewsItem::toMarkdown)
         .ifPresent(System.out::println);
   }

}
