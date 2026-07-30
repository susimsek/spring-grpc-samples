package io.github.susimsek.springgrpcsamples.exception;

import build.buf.validate.Violation;
import java.util.Arrays;

public record GrpcViolation(Violation violation, String messageCode, Object[] messageArguments) {

    private static final String CUSTOM_RULE_ID_PREFIX = "grpc.";
    private static final String STANDARD_RULE_MESSAGE_CODE_PREFIX = "grpc.validation.constraints.";
    private static final String UNKNOWN_MESSAGE_CODE = "grpc.validation.unknown";

    public static GrpcViolation from(build.buf.protovalidate.Violation violation) {
        Violation proto = violation.toProto();
        Object[] args =
                isStandardRule(proto)
                        ? new Object[] {violation.getRuleValue().getValue()}
                        : new Object[0];
        return new GrpcViolation(proto, resolveMessageCode(proto), args);
    }

    private static boolean isStandardRule(Violation violation) {
        return violation.hasRuleId() && !violation.getRuleId().startsWith(CUSTOM_RULE_ID_PREFIX);
    }

    private static String resolveMessageCode(Violation violation) {
        if (!violation.hasRuleId()) {
            return UNKNOWN_MESSAGE_CODE;
        }
        String ruleId = violation.getRuleId();
        boolean standardRule = !ruleId.startsWith(CUSTOM_RULE_ID_PREFIX);
        return standardRule ? STANDARD_RULE_MESSAGE_CODE_PREFIX + ruleId : ruleId;
    }

    @Override
    public Object[] messageArguments() {
        return messageArguments;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        return object
                        instanceof
                        GrpcViolation(Violation violation1, String code, Object[] arguments)
                && violation.equals(violation1)
                && messageCode.equals(code)
                && Arrays.equals(messageArguments, arguments);
    }

    @Override
    public int hashCode() {
        int result = violation.hashCode();
        result = 31 * result + messageCode.hashCode();
        result = 31 * result + Arrays.hashCode(messageArguments);
        return result;
    }

    @Override
    public String toString() {
        return "GrpcViolation[violation="
                + violation
                + ", messageCode="
                + messageCode
                + ", messageArguments="
                + Arrays.toString(messageArguments)
                + "]";
    }
}
