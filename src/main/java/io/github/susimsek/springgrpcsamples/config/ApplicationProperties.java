package io.github.susimsek.springgrpcsamples.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private Security security = new Security();

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
