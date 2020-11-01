package news.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor(staticName = "apply")
public class NewsItemSummary {

   String type;

   String source;

   String title;

   Instant crawled;

   String hash;

}
