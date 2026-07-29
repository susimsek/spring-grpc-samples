package io.github.susimsek.springgrpcsamples.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    @Test
    void createsDatabaseConfig() {
        assertThat(new DatabaseConfig()).isNotNull();
    }
}
