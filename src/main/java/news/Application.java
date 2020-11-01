package news;

import alpakkeer.core.util.ObjectMapperFactory;
import news.adapter.FileSystemNewsItemStore;

import java.nio.file.Path;

public final class Application {

   public static void main(String ...args) {
      var om = ObjectMapperFactory.apply().create(true);
      var config = ApplicationConfiguration.apply();
      var store = FileSystemNewsItemStore.apply(Path.of(config.getDataPath()), om);

      CoreApplication.apply(config, store).run();
   }

}
