package io.github.susimsek.springgrpcsamples.exception;

import build.buf.validate.Violation;

public record GrpcViolation(Violation violation, String messageCode, Object[] messageArguments) {

    private static final String CUSTOM_RULE_ID_PREFIX = "grpc.";
    private static final String STANDARD_RULE_MESSAGE_CODE_PREFIX = "grpc.validation.constraints.";
    private static final String UNKNOWN_MESSAGE_CODE = "grpc.validation.unknown";

    public GrpcViolation(Violation violation) {
        this(resolveRule(violation));
    }

    public GrpcViolation(build.buf.protovalidate.Violation violation) {
        this(resolveRule(violation));
    }

    private GrpcViolation(Rule rule) {
        this(rule.violation(), rule.messageCode(), rule.messageArguments());
    }

    private static Rule resolveRule(Violation violation) {
        if (!violation.hasRuleId()) {
            return new Rule(violation, UNKNOWN_MESSAGE_CODE, false, new Object[0]);
        }
        String ruleId = violation.getRuleId();
        boolean standardRule = !ruleId.startsWith(CUSTOM_RULE_ID_PREFIX);
        String messageCode = standardRule ? STANDARD_RULE_MESSAGE_CODE_PREFIX + ruleId : ruleId;
        return new Rule(violation, messageCode, standardRule, new Object[0]);
    }

    private static Rule resolveRule(build.buf.protovalidate.Violation violation) {
        Violation protoViolation = violation.toProto();
        Rule rule = resolveRule(protoViolation);
        Object[] messageArguments =
                rule.standardRule()
                        ? new Object[] {violation.getRuleValue().getValue()}
                        : new Object[0];
        return new Rule(protoViolation, rule.messageCode(), rule.standardRule(), messageArguments);
    }

    private record Rule(
            Violation violation,
            String messageCode,
            boolean standardRule,
            Object[] messageArguments) {}
}
