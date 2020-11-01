package news.adapter;

import akka.Done;
import alpakkeer.core.util.Operators;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import news.model.NewsItem;
import news.model.NewsItemSummary;
import news.ports.NewsItemStorePort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileSystemNewsItemStore implements NewsItemStorePort {

   private final Path directory;

   private final ObjectMapper om;

   public static FileSystemNewsItemStore apply(Path directory, ObjectMapper om) {
      Operators.suppressExceptions(() -> {
         Files.createDirectories(directory);
      });

      return new FileSystemNewsItemStore(directory, om);
   }

   private Path getNewsItemFileFromHash(String hash) {
      return directory.resolve(hash + ".json");
   }

   private Path getNewsItemFile(NewsItem item) {
      return getNewsItemFileFromHash(item.getHash());
   }

   private NewsItem getNewsItemFromFile(Path file) {
      return Operators.suppressExceptions(() -> om.readValue(file.toFile(), NewsItem.class));
   }

   @Override
   public CompletionStage<Done> insertOrUpdateNewsItem(NewsItem item) {
      var file = getNewsItemFile(item);

      if (!Files.exists(file)) {
         Operators.suppressExceptions(() -> {
            try (var os = Files.newOutputStream(file)) {
               om.writeValue(os, item);
            }
         });
      }

      return CompletableFuture.completedFuture(Done.getInstance());
   }

   @Override
   public CompletionStage<List<NewsItemSummary>> findAll() {
      var result = Operators.suppressExceptions(() -> Files
         .list(directory)
         .map(this::getNewsItemFromFile)
         .map(NewsItem::toSummary)
         .collect(Collectors.toList()));

      return CompletableFuture.completedFuture(result);
   }

   @Override
   public CompletionStage<Optional<NewsItem>> findNewsItemByHash(String hash) {
      var file = getNewsItemFileFromHash(hash);

      if (Files.isRegularFile(file)) {
         var item = Operators.suppressExceptions(() -> om.readValue(file.toFile(), NewsItem.class));
         return CompletableFuture.completedFuture(Optional.of(item));
      } else {
         return CompletableFuture.completedFuture(Optional.empty());
      }
   }

}
