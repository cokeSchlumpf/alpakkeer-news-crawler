package news.ports;

import akka.Done;
import news.model.NewsItem;
import news.model.NewsItemSummary;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface NewsItemStorePort {

   CompletionStage<Done> insertOrUpdateNewsItem(NewsItem item);

   CompletionStage<List<NewsItemSummary>> findAll();

   CompletionStage<Optional<NewsItem>> findNewsItemByHash(String hash);

}
