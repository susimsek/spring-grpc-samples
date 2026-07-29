package io.github.susimsek.springgrpcsamples.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import org.junit.jupiter.api.Test;

class GrpcValidationConfigTest {

    @Test
    void createsGrpcValidator() throws Exception {
        var validator = new GrpcValidationConfig().grpcValidator();

        assertThat(
                        validator
                                .validate(CreateTodoRequest.newBuilder().setTitle("Valid").build())
                                .isSuccess())
                .isTrue();
    }
}
