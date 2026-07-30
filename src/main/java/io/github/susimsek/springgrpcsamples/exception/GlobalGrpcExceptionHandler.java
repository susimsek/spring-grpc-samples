package io.github.susimsek.springgrpcsamples.exception;

import build.buf.validate.FieldPathElement;
import build.buf.validate.Violation;
import com.google.protobuf.Any;
import com.google.rpc.BadRequest;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.protobuf.StatusProto;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.util.StringUtils;

@GrpcAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalGrpcExceptionHandler {

    private final MessageSource messageSource;

    @GrpcExceptionHandler(GrpcApiException.class)
    public StatusException handleGrpcApiException(GrpcApiException exception) {
        return Status.fromCode(exception.getStatusCode())
                .withDescription(resolveMessage(exception))
                .asException();
    }

    @GrpcExceptionHandler(GrpcValidationException.class)
    public StatusException handleGrpcValidationException(GrpcValidationException exception) {
        BadRequest.Builder badRequest = BadRequest.newBuilder();
        exception.getViolations().stream()
                .map(this::toFieldViolation)
                .forEach(badRequest::addFieldViolations);

        com.google.rpc.Status status =
                com.google.rpc.Status.newBuilder()
                        .setCode(com.google.rpc.Code.INVALID_ARGUMENT_VALUE)
                        .setMessage(
                                resolveMessage(
                                        "grpc.validation.failed",
                                        "One or more validation errors occurred."))
                        .addDetails(Any.pack(badRequest.build()))
                        .build();

        return StatusProto.toStatusException(status);
    }

    @GrpcExceptionHandler(InvalidBearerTokenException.class)
    public StatusException handleInvalidBearerTokenException(
            InvalidBearerTokenException exception) {
        return Status.UNAUTHENTICATED
                .withDescription(
                        resolveMessage("grpc.auth.invalidToken", "Invalid or expired token."))
                .asException();
    }

    @GrpcExceptionHandler(AuthenticationException.class)
    public StatusException handleAuthenticationException(AuthenticationException exception) {
        return Status.UNAUTHENTICATED
                .withDescription(
                        resolveMessage("grpc.auth.unauthenticated", "Authentication failed."))
                .asException();
    }

    @GrpcExceptionHandler(AccessDeniedException.class)
    public StatusException handleAccessDeniedException(AccessDeniedException exception) {
        return Status.PERMISSION_DENIED
                .withDescription(resolveMessage("grpc.auth.accessDenied", "Access denied."))
                .asException();
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusException handleException(Exception exception) {
        log.error("Unhandled gRPC exception", exception);
        return Status.INTERNAL
                .withDescription(
                        resolveMessage(
                                "grpc.internal",
                                "An unexpected error occurred. Please try again later."))
                .asException();
    }

    private String resolveMessage(GrpcApiException exception) {
        return resolveMessage(
                exception.getMessageCode(),
                exception.getMessage(),
                exception.getMessageArguments());
    }

    private String resolveMessage(
            String messageCode, String defaultMessage, Object... messageArguments) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(messageCode, messageArguments, defaultMessage, locale);
    }

    private BadRequest.FieldViolation toFieldViolation(GrpcViolation validationViolation) {
        Violation violation = validationViolation.violation();
        return BadRequest.FieldViolation.newBuilder()
                .setField(resolveField(violation))
                .setDescription(resolveValidationMessage(validationViolation))
                .build();
    }

    private String resolveValidationMessage(GrpcViolation validationViolation) {
        Violation violation = validationViolation.violation();
        String defaultMessage = violation.hasMessage() ? violation.getMessage() : "invalid value";
        return resolveMessage(
                validationViolation.messageCode(),
                defaultMessage,
                validationViolation.messageArguments());
    }

    private static String resolveField(Violation violation) {
        if (!violation.hasField() || violation.getField().getElementsCount() == 0) {
            return resolveMessageLevelField();
        }
        return violation.getField().getElementsList().stream()
                .map(GlobalGrpcExceptionHandler::resolveFieldElement)
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "." + right)
                .orElseGet(GlobalGrpcExceptionHandler::resolveMessageLevelField);
    }

    private static String resolveFieldElement(FieldPathElement element) {
        if (element.hasFieldName()) {
            return element.getFieldName();
        }
        if (element.hasFieldNumber()) {
            return String.valueOf(element.getFieldNumber());
        }
        return "";
    }

    private static String resolveMessageLevelField() {
        return "request";
    }
}
