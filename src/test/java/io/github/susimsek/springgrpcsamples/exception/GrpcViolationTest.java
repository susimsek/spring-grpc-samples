package io.github.susimsek.springgrpcsamples.exception;

import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.Violation;
import com.google.protobuf.Message;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
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
        assertThat(violation.messageArguments()[0]).isNotNull();
    }

    @Test
    void equalsAndHashCodeUseArrayContent() {
        Violation proto = Violation.newBuilder().setRuleId("string.min_len").build();
        GrpcViolation first = new GrpcViolation(proto, "code", new Object[] {"x", 1});
        GrpcViolation second =
                new GrpcViolation(proto.toBuilder().build(), "code", new Object[] {"x", 1});
        GrpcViolation differentArray =
                new GrpcViolation(proto.toBuilder().build(), "code", new Object[] {"x", 2});

        assertThat(first).isEqualTo(first);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first).isNotEqualTo(differentArray);
        assertThat(first).isNotEqualTo("not-a-violation");
    }

    @Test
    void toStringPrintsArrayContent() {
        GrpcViolation violation =
                new GrpcViolation(Violation.newBuilder().build(), "code", new Object[] {"x", 1});

        assertThat(violation.toString())
                .contains("GrpcViolation[")
                .contains("messageCode=code")
                .contains("messageArguments=[x, 1]");
    }

    @Test
    void protectsMessageArgumentsWithDefensiveCopies() {
        Object[] arguments = {"x"};
        GrpcViolation violation =
                new GrpcViolation(Violation.newBuilder().build(), "code", arguments);

        arguments[0] = "changed";
        Object[] exposedArguments = violation.messageArguments();
        exposedArguments[0] = "also-changed";

        assertThat(violation.messageArguments()).containsExactly("x");
    }

    private ValidationResult validate(Message message) {
        try {
            return validator.validate(message);
        } catch (ValidationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
