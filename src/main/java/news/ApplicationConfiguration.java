package news;

import alpakkeer.core.config.Configs;
import alpakkeer.core.config.annotations.ConfigurationProperties;
import alpakkeer.core.config.annotations.Value;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@ConfigurationProperties
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor(staticName = "apply")
public final class ApplicationConfiguration {

   @Value("data-path")
   String dataPath;

   @Value("export-path")
   String exportPath;

   @Value("seeds--ard-teletext")
   List<String> seedsARDTeletext;

   @Value("seeds--spiegel-online")
   List<String> seedsSpiegelOnline;

   public static ApplicationConfiguration apply() {
      return Configs.mapToConfigClass(ApplicationConfiguration.class, "app.conf");
   }

}
