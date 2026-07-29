package io.github.susimsek.springgrpcsamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class SpringGrpcSamplesApplicationTest {

    @Test
    void constructorCreatesApplication() {
        assertThat(new SpringGrpcSamplesApplication()).isNotNull();
    }

    @Test
    void mainRunsSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication =
                mockStatic(SpringApplication.class)) {
            springApplication
                    .when(() -> SpringApplication.run(SpringGrpcSamplesApplication.class, args))
                    .thenReturn(null);

            SpringGrpcSamplesApplication.main(args);

            springApplication.verify(
                    () -> SpringApplication.run(SpringGrpcSamplesApplication.class, args));
        }
    }
}
