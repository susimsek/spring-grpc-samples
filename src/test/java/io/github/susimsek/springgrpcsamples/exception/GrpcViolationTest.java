package io.github.susimsek.springgrpcsamples.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        GrpcViolation violation =
                new GrpcViolation(
                        Violation.newBuilder().build(), "grpc.validation.unknown", new Object[0]);

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.unknown");
        assertThat(violation.messageArguments()).isEmpty();
    }

    @Test
    void resolvesCustomAndStandardMessageCodesFromProtoViolation() {
        GrpcViolation custom =
                new GrpcViolation(
                        Violation.newBuilder().setRuleId("grpc.validation.custom").build(),
                        "grpc.validation.custom",
                        new Object[0]);
        GrpcViolation standard =
                new GrpcViolation(
                        Violation.newBuilder().setRuleId("string.min_len").build(),
                        "grpc.validation.constraints.string.min_len",
                        new Object[0]);

        assertThat(custom.messageCode()).isEqualTo("grpc.validation.custom");
        assertThat(standard.messageCode()).isEqualTo("grpc.validation.constraints.string.min_len");
    }

    @Test
    void resolvesUnknownMessageCodeWhenRuleIdMissingFromProtovalidateViolation() {
        build.buf.protovalidate.Violation protoViolation =
                mock(build.buf.protovalidate.Violation.class);
        when(protoViolation.toProto()).thenReturn(Violation.newBuilder().build());

        GrpcViolation violation = GrpcViolation.from(protoViolation);

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.unknown");
        assertThat(violation.messageArguments()).isEmpty();
    }

    @Test
    void resolvesRuleValueArgumentForProtovalidateViolation() {
        build.buf.protovalidate.Violation protoViolation =
                validate(CreateTodoRequest.newBuilder().setTitle("ab").build())
                        .getViolations()
                        .getFirst();

        GrpcViolation violation = GrpcViolation.from(protoViolation);

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.constraints.string.min_len");
        assertThat(violation.messageArguments()).hasSize(1);
        assertThat(violation.messageArguments()[0]).isNotNull();
    }

    @Test
    void doesNotExposeRuleValueArgumentForCustomProtovalidateViolation() {
        build.buf.protovalidate.Violation protoViolation =
                mock(build.buf.protovalidate.Violation.class);
        when(protoViolation.toProto())
                .thenReturn(Violation.newBuilder().setRuleId("grpc.validation.custom").build());

        GrpcViolation violation = GrpcViolation.from(protoViolation);

        assertThat(violation.messageCode()).isEqualTo("grpc.validation.custom");
        assertThat(violation.messageArguments()).isEmpty();
    }

    @Test
    void equalsAndHashCodeUseListContent() {
        Violation proto = Violation.newBuilder().setRuleId("string.min_len").build();
        GrpcViolation first = new GrpcViolation(proto, "code", new Object[] {"x", 1});
        GrpcViolation second =
                new GrpcViolation(proto.toBuilder().build(), "code", new Object[] {"x", 1});
        GrpcViolation differentViolation =
                new GrpcViolation(
                        Violation.newBuilder().setRuleId("string.max_len").build(),
                        "code",
                        new Object[] {"x", 1});
        GrpcViolation differentMessageCode =
                new GrpcViolation(proto.toBuilder().build(), "other", new Object[] {"x", 1});
        GrpcViolation differentArguments =
                new GrpcViolation(proto.toBuilder().build(), "code", new Object[] {"x", 2});

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
                new GrpcViolation(Violation.newBuilder().build(), "code", new Object[] {"x", 1});

        assertThat(violation.toString())
                .contains("GrpcViolation[")
                .contains("messageCode=code")
                .contains("messageArguments=[x, 1]");
    }

    @Test
    void messageArgumentsReturnsStoredArray() {
        GrpcViolation violation =
                new GrpcViolation(Violation.newBuilder().build(), "code", new Object[] {"x"});

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
