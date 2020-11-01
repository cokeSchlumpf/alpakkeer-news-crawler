package news.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor(staticName = "apply")
public class SpiegelNewsItem implements NewsItem {

   String source;

   String title;

   String description;

   String text;

   String hash;

   Instant crawled;

   public String toMarkdown() {
      return "# " + title +
         "\n\n" +
         description +
         "\n\n" +
         "---" +
         "\n\n" +
         text +
         "\n\n" +
         "---" +
         "\n\n" +
         "[^source](" + source + ")";
   }

   @Override
   public NewsItemSummary toSummary() {
      return NewsItemSummary.apply("Spiegel Online", source, title, crawled, hash);
   }

}
