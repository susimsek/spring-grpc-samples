package io.github.susimsek.springgrpcsamples.config;

import io.github.susimsek.springgrpcsamples.security.SecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class DatabaseConfig {

    @Bean
    AuditorAware<String> auditorAware() {
        return new SecurityAuditorAware();
    }
}
