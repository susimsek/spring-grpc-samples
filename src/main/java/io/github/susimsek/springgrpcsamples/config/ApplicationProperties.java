package io.github.susimsek.springgrpcsamples.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Cache cache = new Cache();

    private Security security = new Security();

    @Getter
    @Setter
    public static class Cache {

        private Caffeine caffeine = new Caffeine();
    }

    @Getter
    @Setter
    public static class Caffeine {

        private Duration ttl = Duration.ofMinutes(10);

        private int initialCapacity = 100;

        private long maximumSize = 1_000;
    }

    @Getter
    @Setter
    public static class Security {

        private Jwt jwt = new Jwt();
    }

    @Getter
    @Setter
    public static class Jwt {

        private String issuer;

        private String secret;

        private Duration expiresIn;
    }
}
