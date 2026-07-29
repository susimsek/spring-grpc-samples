package io.github.susimsek.springgrpcsamples.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.config.ApplicationProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTest {

    @Test
    void generateTokenCreatesSignedJwt() {
        SecretKey secretKey =
                new SecretKeySpec(
                        "test-secret-key-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256");
        JwtEncoder encoder =
                NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
        JwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.getSecurity().getJwt().setIssuer("https://test-issuer");
        applicationProperties.getSecurity().getJwt().setExpiresIn(Duration.ofHours(1));
        JwtService jwtService = new JwtService(encoder, applicationProperties);

        String token =
                jwtService.generateToken(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "admin",
                                null,
                                List.of(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))));
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getIssuer().toString()).isEqualTo("https://test-issuer");
        assertThat(decoded.getSubject()).isEqualTo("admin");
        assertThat(decoded.getClaimAsStringList(SecurityUtils.AUTHORITIES_CLAIM))
                .containsExactly(AuthoritiesConstants.ADMIN);
        assertThat(jwtService.getExpiresInSeconds()).isEqualTo(3600L);
    }
}
