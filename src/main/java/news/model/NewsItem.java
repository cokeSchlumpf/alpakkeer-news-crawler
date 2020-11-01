package news.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(
   use = JsonTypeInfo.Id.NAME,
   property = "type")
@JsonSubTypes(
   {
      @JsonSubTypes.Type(value = ARDNewsItem.class, name = "ard-teletext"),
      @JsonSubTypes.Type(value = SpiegelNewsItem.class, name = "spiegel-online")
   })
public interface NewsItem {

   String getTitle();

   String getText();

   Instant getCrawled();

   String getSource();

   String getHash();

   String toMarkdown();

   NewsItemSummary toSummary();

}
