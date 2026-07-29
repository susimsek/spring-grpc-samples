package io.github.susimsek.springgrpcsamples.security;

import io.github.susimsek.springgrpcsamples.config.ApplicationProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration expiresIn;

    public JwtService(JwtEncoder jwtEncoder, ApplicationProperties applicationProperties) {
        this.jwtEncoder = jwtEncoder;
        ApplicationProperties.Jwt jwtProperties = applicationProperties.getSecurity().getJwt();
        this.issuer = jwtProperties.getIssuer();
        this.expiresIn = jwtProperties.getExpiresIn();
    }

    public String generateToken(Authentication authentication) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .issuedAt(issuedAt)
                        .expiresAt(issuedAt.plus(expiresIn))
                        .subject(authentication.getName())
                        .claim(SecurityUtils.AUTHORITIES_CLAIM, resolveRoles(authentication))
                        .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiresInSeconds() {
        return expiresIn.toSeconds();
    }

    private static List<String> resolveRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
