package io.github.susimsek.springgrpcsamples.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.google.protobuf.Message;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrpcViolationTest {

    private final Validator validator = ValidatorFactory.newBuilder().build();

    @Test
    void resolvesUnknownMessageCodeWhenRuleIdMissing() {
        GrpcViolation violation = new GrpcViolation(Violation.newBuilder().build());

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.unknown");
        assertThat(violation.messageArguments()).isEmpty();
    }

    @Test
    void resolvesCustomAndStandardMessageCodesFromProtoViolation() {
        GrpcViolation custom =
                new GrpcViolation(
                        Violation.newBuilder().setRuleId("grpc.validation.custom").build());
        GrpcViolation standard =
                new GrpcViolation(Violation.newBuilder().setRuleId("string.min_len").build());

        assertThat(custom.messageCode()).isEqualTo("grpc.validation.custom");
        assertThat(standard.messageCode()).isEqualTo("grpc.validation.constraints.string.min_len");
    }

    @Test
    void resolvesRuleValueArgumentForProtovalidateViolation() {
        build.buf.protovalidate.Violation protoViolation =
                validate(CreateTodoRequest.newBuilder().setTitle("ab").build())
                        .getViolations()
                        .getFirst();

        GrpcViolation violation = new GrpcViolation(protoViolation);

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.constraints.string.min_len");
        assertThat(violation.messageArguments()).hasSize(1);
        assertThat(violation.messageArguments().getFirst()).isNotNull();
    }

    @Test
    void doesNotExposeRuleValueArgumentForCustomProtovalidateViolation() {
        build.buf.protovalidate.Violation protoViolation =
                mock(build.buf.protovalidate.Violation.class);
        when(protoViolation.toProto())
                .thenReturn(Violation.newBuilder().setRuleId("grpc.validation.custom").build());

        GrpcViolation violation = new GrpcViolation(protoViolation);

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.custom");
        assertThat(violation.messageArguments()).isEmpty();
    }

    @Test
    void equalsAndHashCodeUseListContent() {
        Violation proto = Violation.newBuilder().setRuleId("string.min_len").build();
        GrpcViolation first = new GrpcViolation(proto, "code", List.of("x", 1));
        GrpcViolation second =
                new GrpcViolation(proto.toBuilder().build(), "code", List.of("x", 1));
        GrpcViolation differentViolation =
                new GrpcViolation(
                        Violation.newBuilder().setRuleId("string.max_len").build(),
                        "code",
                        List.of("x", 1));
        GrpcViolation differentMessageCode =
                new GrpcViolation(proto.toBuilder().build(), "other", List.of("x", 1));
        GrpcViolation differentArguments =
                new GrpcViolation(proto.toBuilder().build(), "code", List.of("x", 2));

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first).isNotEqualTo(differentViolation);
        assertThat(first).isNotEqualTo(differentMessageCode);
        assertThat(first).isNotEqualTo(differentArguments);
        assertThat(first).isNotEqualTo("not-a-violation");
    }

    @Test
    void toStringPrintsListContent() {
        GrpcViolation violation =
                new GrpcViolation(Violation.newBuilder().build(), "code", List.of("x", 1));

        assertThat(violation.toString())
                .contains("GrpcViolation[")
                .contains("messageCode=code")
                .contains("messageArguments=[x, 1]");
    }

    @Test
    void protectsMessageArgumentsWithDefensiveCopies() {
        List<Object> arguments = new ArrayList<>(List.of("x"));
        GrpcViolation violation =
                new GrpcViolation(Violation.newBuilder().build(), "code", arguments);

        arguments.set(0, "changed");

        assertThat(violation.messageArguments()).containsExactly("x");
        assertThatThrownBy(() -> violation.messageArguments().set(0, "also-changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private ValidationResult validate(Message message) {
        try {
            return validator.validate(message);
        } catch (ValidationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
