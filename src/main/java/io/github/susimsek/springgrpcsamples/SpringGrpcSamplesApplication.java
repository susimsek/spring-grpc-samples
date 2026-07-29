package io.github.susimsek.springgrpcsamples;

import io.github.susimsek.springgrpcsamples.config.aot.NativeRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportRuntimeHints(NativeRuntimeHints.class)
public class SpringGrpcSamplesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringGrpcSamplesApplication.class, args);
    }
}
