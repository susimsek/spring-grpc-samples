package io.github.susimsek.springgrpcsamples.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class GrpcValidationException extends RuntimeException {

    private final List<GrpcViolation> violations;

    public GrpcValidationException(List<GrpcViolation> violations) {
        super("gRPC request validation failed");
        this.violations = List.copyOf(violations);
    }
}
