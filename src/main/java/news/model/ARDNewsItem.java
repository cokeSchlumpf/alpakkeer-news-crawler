package news.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor(staticName = "apply")
public class ARDNewsItem implements NewsItem {

   String source;

   String title;

   String text;

   String hash;

   Instant crawled;

   public String toMarkdown() {
      return "# " + title +
         "\n\n" +
         text +
         "\n\n" +
         "---" +
         "\n\n" +
         "[^source](" + source + ")";
   }

   @Override
   public NewsItemSummary toSummary() {
      return NewsItemSummary.apply("ARD Teletext", source, title, crawled, hash);
   }

}
