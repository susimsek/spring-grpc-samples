package io.github.susimsek.springgrpcsamples.config.validation;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import io.github.susimsek.springgrpcsamples.exception.GrpcValidationException;
import io.github.susimsek.springgrpcsamples.exception.GrpcViolation;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

@Component
@GlobalServerInterceptor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class GrpcValidationServerInterceptor implements ServerInterceptor {

    private static final String APPLICATION_PROTO_PACKAGE =
            "io.github.susimsek.springgrpcsamples.proto";

    private final Validator validator;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ValidationServerCallListener<>(listener, validator);
    }

    private static final class ValidationServerCallListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final Validator validator;

        private ValidationServerCallListener(
                ServerCall.Listener<ReqT> delegate, Validator validator) {
            super(delegate);
            this.validator = validator;
        }

        @Override
        public void onMessage(ReqT message) {
            if (message instanceof Message protoMessage
                    && APPLICATION_PROTO_PACKAGE.equals(protoMessage.getClass().getPackageName())) {
                validate(protoMessage);
            }
            super.onMessage(message);
        }

        private void validate(Message message) {
            try {
                ValidationResult result = validator.validate(message);
                if (!result.isSuccess()) {
                    throw new GrpcValidationException(
                            result.getViolations().stream()
                                    .map(v -> GrpcViolation.from(v))
                                    .toList());
                }
            } catch (ValidationException ex) {
                throw new IllegalStateException("Failed to validate gRPC request", ex);
            }
        }
    }
}
