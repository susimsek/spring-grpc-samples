package io.github.susimsek.springgrpcsamples.exception;

import io.grpc.Status;
import lombok.Getter;

@Getter
public abstract class GrpcApiException extends RuntimeException {

    private final Status.Code statusCode;
    private final String messageCode;
    private final Object[] messageArguments;

    protected GrpcApiException(
            Status.Code statusCode,
            String messageCode,
            String defaultMessage,
            Object... messageArguments) {
        super(defaultMessage);
        this.statusCode = statusCode;
        this.messageCode = messageCode;
        this.messageArguments = messageArguments;
    }
}
