package watchmyride;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watchmyride")
public record OwnerSettings(String ownerToken) {
}
