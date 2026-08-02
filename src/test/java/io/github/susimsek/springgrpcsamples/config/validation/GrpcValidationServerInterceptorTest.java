package io.github.susimsek.springgrpcsamples.config.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.github.susimsek.springgrpcsamples.exception.GrpcValidationException;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.ListTodosRequest;
import io.github.susimsek.springgrpcsamples.proto.PageRequest;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GrpcValidationServerInterceptorTest {

    @Mock private ServerCall<Object, Object> call;

    @Mock private ServerCallHandler<Object, Object> handler;

    @Mock private ServerCall.Listener<Object> delegate;

    @Mock private Validator failingValidator;

    @Test
    void delegatesValidProtoMessages() {
        Object request = CreateTodoRequest.newBuilder().setTitle("Valid").build();
        ServerCall.Listener<Object> listener = listener(ValidatorFactory.newBuilder().build());

        listener.onMessage(request);

        verify(delegate).onMessage(request);
    }

    @Test
    void rejectsInvalidProtoMessages() {
        Object request = CreateTodoRequest.newBuilder().build();
        ServerCall.Listener<Object> listener = listener(ValidatorFactory.newBuilder().build());

        assertThatThrownBy(() -> listener.onMessage(request))
                .isInstanceOf(GrpcValidationException.class);
    }

    @Test
    void mapsStringRuleValuesToMessageArguments() {
        Object request = CreateTodoRequest.newBuilder().setTitle("ab").build();
        ServerCall.Listener<Object> listener = listener(ValidatorFactory.newBuilder().build());

        Throwable thrown = catchThrowable(() -> listener.onMessage(request));

        assertViolationArguments(thrown, "3");
    }

    @Test
    void mapsInt64RuleValuesToMessageArguments() {
        Object request = GetTodoRequest.newBuilder().build();
        ServerCall.Listener<Object> listener = listener(ValidatorFactory.newBuilder().build());

        Throwable thrown = catchThrowable(() -> listener.onMessage(request));

        assertViolationArguments(thrown, "0");
    }

    @Test
    void mapsInt32RuleValuesToMessageArguments() {
        Object request =
                ListTodosRequest.newBuilder()
                        .setPageRequest(PageRequest.newBuilder().setPage(-1).build())
                        .build();
        ServerCall.Listener<Object> listener = listener(ValidatorFactory.newBuilder().build());

        Throwable thrown = catchThrowable(() -> listener.onMessage(request));

        assertViolationArguments(thrown, "0");
    }

    @Test
    void delegatesNonProtoMessages() {
        Object request = "not-proto";
        ServerCall.Listener<Object> listener = listener(ValidatorFactory.newBuilder().build());

        listener.onMessage(request);

        verify(delegate).onMessage(request);
    }

    @Test
    void delegatesNonApplicationProtoMessagesWithoutValidation() throws Exception {
        Object request = StringValue.of("framework-message");
        ServerCall.Listener<Object> listener = listener(failingValidator);

        listener.onMessage(request);

        verify(failingValidator, never()).validate(any(Message.class));
        verify(delegate).onMessage(request);
    }

    @Test
    void wrapsValidationEngineFailures() throws Exception {
        when(failingValidator.validate(any(Message.class)))
                .thenThrow(new ValidationException("boom"));
        Object request = CreateTodoRequest.newBuilder().setTitle("Valid").build();
        ServerCall.Listener<Object> listener = listener(failingValidator);

        assertThatThrownBy(() -> listener.onMessage(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to validate gRPC request")
                .hasCauseInstanceOf(ValidationException.class);
    }

    private ServerCall.Listener<Object> listener(Validator validator) {
        when(handler.startCall(same(call), any(Metadata.class))).thenReturn(delegate);
        return new GrpcValidationServerInterceptor(validator)
                .interceptCall(call, new Metadata(), handler);
    }

    private static void assertViolationArguments(Throwable thrown, String expectedArgument) {
        assertThat(thrown)
                .isInstanceOfSatisfying(
                        GrpcValidationException.class,
                        exception ->
                                assertThat(exception.getViolations())
                                        .singleElement()
                                        .satisfies(
                                                violation ->
                                                        assertThat(violation.messageArguments())
                                                                .extracting(Object::toString)
                                                                .containsExactly(
                                                                        expectedArgument)));
    }
}
