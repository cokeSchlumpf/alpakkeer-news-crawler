package news.crawler;

import news.model.NewsItem;

import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface Crawler {

   CompletionStage<Set<String>> getNewsItems(String clusterUrl);

   CompletionStage<NewsItem> extractNewsItem(String newsItemUrl);

}
