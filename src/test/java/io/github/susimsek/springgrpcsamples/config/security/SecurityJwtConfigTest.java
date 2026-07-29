package io.github.susimsek.springgrpcsamples.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.config.ApplicationProperties;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class SecurityJwtConfigTest {

    private final SecurityJwtConfig config = new SecurityJwtConfig();

    @Test
    void createsJwtBeans() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties
                .getSecurity()
                .getJwt()
                .setSecret("test-secret-key-with-at-least-32-bytes");
        SecretKey secretKey = config.jwtSecretKey(applicationProperties);

        assertThat(secretKey.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(config.jwtEncoder(secretKey)).isInstanceOf(JwtEncoder.class);
        assertThat(config.jwtDecoder(secretKey)).isInstanceOf(JwtDecoder.class);
    }

    @Test
    void createsJwtAuthenticationConverterBean() {
        assertThat(config.jwtAuthenticationConverter())
                .isInstanceOf(JwtAuthenticationConverter.class);
    }
}
