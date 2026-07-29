package io.github.susimsek.springgrpcsamples.exception;

import static org.assertj.core.api.Assertions.assertThat;

import build.buf.protovalidate.ValidationResult;
import build.buf.protovalidate.Validator;
import build.buf.protovalidate.ValidatorFactory;
import build.buf.protovalidate.exceptions.ValidationException;
import build.buf.validate.FieldPath;
import build.buf.validate.FieldPathElement;
import build.buf.validate.Violation;
import com.google.protobuf.Message;
import com.google.rpc.BadRequest;
import io.github.susimsek.springgrpcsamples.proto.CreateTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.GetTodoRequest;
import io.github.susimsek.springgrpcsamples.proto.PatchTodoRequest;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class GlobalGrpcExceptionHandlerTest {

    private final GlobalGrpcExceptionHandler handler =
            new GlobalGrpcExceptionHandler(messageSource());
    private final Validator validator = ValidatorFactory.newBuilder().build();

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void mapsApplicationExceptionsToGrpcStatuses() {
        assertStatus(
                handler.handleGrpcApiException(new TodoNotFoundException(1L)),
                Status.Code.NOT_FOUND,
                "todo not found with id: 1");
    }

    @Test
    void resolvesTurkishMessages() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));

        assertStatus(
                handler.handleGrpcApiException(new TodoNotFoundException(1L)),
                Status.Code.NOT_FOUND,
                "id'si 1 olan todo bulunamad\u0131");
    }

    @Test
    void mapsGrpcValidationExceptionToBadRequestDetails() throws Exception {
        StatusException exception =
                handler.handleGrpcValidationException(
                        validationException(
                                CreateTodoRequest.newBuilder().build(),
                                GetTodoRequest.newBuilder().build(),
                                PatchTodoRequest.newBuilder().setId(1L).build()));

        assertStatus(
                exception, Status.Code.INVALID_ARGUMENT, "One or more validation errors occurred.");
        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .extracting(BadRequest.FieldViolation::getField)
                .containsExactlyInAnyOrder("request", "id", "request");
        assertThat(badRequest.getFieldViolationsList())
                .extracting(BadRequest.FieldViolation::getDescription)
                .containsExactlyInAnyOrder(
                        "This field cannot be blank.",
                        "Value must be greater than 0.",
                        "This field cannot be empty.");
    }

    @Test
    void resolvesTurkishGrpcValidationMessages() throws Exception {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));

        StatusException exception =
                handler.handleGrpcValidationException(
                        validationException(CreateTodoRequest.newBuilder().build()));

        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .singleElement()
                .satisfies(
                        violation -> {
                            assertThat(violation.getField()).isEqualTo("request");
                            assertThat(violation.getDescription())
                                    .isEqualTo("Bu alan bo\u015F b\u0131rak\u0131lamaz.");
                        });
    }

    @Test
    void resolvesMinLengthValidationMessages() throws Exception {
        StatusException exception =
                handler.handleGrpcValidationException(
                        validationException(CreateTodoRequest.newBuilder().setTitle("ab").build()));

        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .singleElement()
                .satisfies(
                        violation -> {
                            assertThat(violation.getField()).isEqualTo("title");
                            assertThat(violation.getDescription())
                                    .isEqualTo("Value length must be at least 3 characters.");
                        });
    }

    @Test
    void resolvesTurkishMaxLengthValidationMessages() throws Exception {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("tr"));

        StatusException exception =
                handler.handleGrpcValidationException(
                        validationException(
                                CreateTodoRequest.newBuilder().setTitle("a".repeat(256)).build()));

        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .singleElement()
                .satisfies(
                        violation -> {
                            assertThat(violation.getField()).isEqualTo("title");
                            assertThat(violation.getDescription())
                                    .isEqualTo(
                                            "Uzunluk en fazla 255 karakter olmal\u0131d\u0131r.");
                        });
    }

    @Test
    void mapsFallbackValidationViolations() throws Exception {
        StatusException exception =
                handler.handleGrpcValidationException(
                        new GrpcValidationException(
                                List.of(
                                        new GrpcViolation(
                                                Violation.newBuilder()
                                                        .setRuleId("custom.unknown")
                                                        .setMessage("fallback message")
                                                        .build()),
                                        new GrpcViolation(Violation.newBuilder().build()))));

        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .extracting(BadRequest.FieldViolation::getField)
                .containsExactly("request", "request");
        assertThat(badRequest.getFieldViolationsList())
                .extracting(BadRequest.FieldViolation::getDescription)
                .containsExactly("fallback message", "invalid value");
    }

    @Test
    void resolvesFieldPathValidationViolations() throws Exception {
        StatusException exception =
                handler.handleGrpcValidationException(
                        new GrpcValidationException(
                                List.of(
                                        new GrpcViolation(
                                                Violation.newBuilder()
                                                        .setField(FieldPath.newBuilder().build())
                                                        .setMessage("empty field message")
                                                        .build()),
                                        new GrpcViolation(
                                                Violation.newBuilder()
                                                        .setField(
                                                                FieldPath.newBuilder()
                                                                        .addElements(
                                                                                FieldPathElement
                                                                                        .newBuilder()
                                                                                        .build())
                                                                        .addElements(
                                                                                FieldPathElement
                                                                                        .newBuilder()
                                                                                        .setFieldName(
                                                                                                "parent")
                                                                                        .build())
                                                                        .addElements(
                                                                                FieldPathElement
                                                                                        .newBuilder()
                                                                                        .setFieldName(
                                                                                                "child")
                                                                                        .build())
                                                                        .build())
                                                        .setMessage("nested message")
                                                        .build()),
                                        new GrpcViolation(
                                                Violation.newBuilder()
                                                        .setField(
                                                                FieldPath.newBuilder()
                                                                        .addElements(
                                                                                FieldPathElement
                                                                                        .newBuilder()
                                                                                        .setFieldNumber(
                                                                                                7)
                                                                                        .build())
                                                                        .build())
                                                        .setMessage("number message")
                                                        .build()),
                                        new GrpcViolation(
                                                Violation.newBuilder()
                                                        .setField(
                                                                FieldPath.newBuilder()
                                                                        .addElements(
                                                                                FieldPathElement
                                                                                        .newBuilder()
                                                                                        .build())
                                                                        .build())
                                                        .setRuleId("string.min_len")
                                                        .setMessage("range message")
                                                        .build()))));

        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .extracting(BadRequest.FieldViolation::getField)
                .containsExactly("request", "parent.child", "7", "request");
    }

    @Test
    void resolvesMessageLevelValidationViolations() throws Exception {
        StatusException exception =
                handler.handleGrpcValidationException(
                        new GrpcValidationException(
                                List.of(
                                        new GrpcViolation(
                                                Violation.newBuilder()
                                                        .setRuleId(
                                                                "grpc.validation.constraints.notBlank")
                                                        .setMessage("blank message")
                                                        .build()))));

        BadRequest badRequest = badRequest(exception);
        assertThat(badRequest.getFieldViolationsList())
                .singleElement()
                .satisfies(
                        violation -> {
                            assertThat(violation.getField()).isEqualTo("request");
                            assertThat(violation.getDescription())
                                    .isEqualTo("This field cannot be blank.");
                        });
    }

    @Test
    void handlesUnhandledExceptionsAsInternalStatus() {
        assertStatus(
                handler.handleException(new IllegalStateException("boom")),
                Status.Code.INTERNAL,
                "An unexpected error occurred. Please try again later.");
    }

    @Test
    void mapsAuthenticationExceptionsToUnauthenticatedStatus() {
        assertStatus(
                handler.handleAuthenticationException(
                        new BadCredentialsException("bad credentials")),
                Status.Code.UNAUTHENTICATED,
                "Authentication failed.");
    }

    @Test
    void mapsInvalidBearerTokenExceptionsToUnauthenticatedStatus() {
        assertStatus(
                handler.handleInvalidBearerTokenException(
                        new InvalidBearerTokenException("bad token")),
                Status.Code.UNAUTHENTICATED,
                "Invalid or expired token.");
    }

    @Test
    void mapsAccessDeniedExceptionsToPermissionDeniedStatus() {
        assertStatus(
                handler.handleAccessDeniedException(new AccessDeniedException("denied")),
                Status.Code.PERMISSION_DENIED,
                "Access denied.");
    }

    private GrpcValidationException validationException(Message... messages) {
        List<GrpcViolation> violations =
                Arrays.stream(messages)
                        .map(this::validate)
                        .flatMap(result -> result.getViolations().stream())
                        .map(GrpcViolation::new)
                        .toList();
        return new GrpcValidationException(violations);
    }

    private ValidationResult validate(Message message) {
        try {
            return validator.validate(message);
        } catch (ValidationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static BadRequest badRequest(StatusException exception) throws Exception {
        com.google.rpc.Status status = StatusProto.fromThrowable(exception);
        assertThat(status).isNotNull();
        assertThat(status.getDetailsList()).hasSize(1);
        return status.getDetails(0).unpack(BadRequest.class);
    }

    private static void assertStatus(
            StatusException exception, Status.Code code, String description) {
        Status status = exception.getStatus();
        assertThat(status.getCode()).isEqualTo(code);
        assertThat(status.getDescription()).isEqualTo(description);
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
