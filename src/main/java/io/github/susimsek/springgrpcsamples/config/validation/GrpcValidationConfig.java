package io.github.susimsek.springgrpcsamples.config.validation;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GrpcValidationConfig {

    @Bean
    public Validator grpcValidator() {
        return ValidatorFactory.newBuilder().build();
    }
}
