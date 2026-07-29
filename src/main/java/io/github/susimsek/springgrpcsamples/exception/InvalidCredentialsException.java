package io.github.susimsek.springgrpcsamples.exception;

import io.grpc.Status;

public class InvalidCredentialsException extends GrpcApiException {

    public InvalidCredentialsException() {
        super(
                Status.Code.UNAUTHENTICATED,
                "grpc.auth.invalidCredentials",
                "invalid username or password");
    }
}
