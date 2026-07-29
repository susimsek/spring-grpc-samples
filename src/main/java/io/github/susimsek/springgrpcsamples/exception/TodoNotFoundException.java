package io.github.susimsek.springgrpcsamples.exception;

import io.grpc.Status;

public class TodoNotFoundException extends GrpcApiException {

    public TodoNotFoundException(Long id) {
        super(
                Status.Code.NOT_FOUND,
                "grpc.todo.notFound",
                "todo not found with id: " + id,
                id.toString());
    }
}
