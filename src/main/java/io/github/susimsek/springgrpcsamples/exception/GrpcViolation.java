package io.github.susimsek.springgrpcsamples.exception;

import build.buf.validate.Violation;
import java.util.List;

public record GrpcViolation(
        Violation violation, String messageCode, List<Object> messageArguments) {

    private static final String CUSTOM_RULE_ID_PREFIX = "grpc.";
    private static final String STANDARD_RULE_MESSAGE_CODE_PREFIX = "grpc.validation.constraints.";
    private static final String UNKNOWN_MESSAGE_CODE = "grpc.validation.unknown";

    public GrpcViolation {
        messageArguments = List.copyOf(messageArguments);
    }

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
            return new Rule(violation, UNKNOWN_MESSAGE_CODE, false, List.of());
        }
        String ruleId = violation.getRuleId();
        boolean standardRule = !ruleId.startsWith(CUSTOM_RULE_ID_PREFIX);
        String messageCode = standardRule ? STANDARD_RULE_MESSAGE_CODE_PREFIX + ruleId : ruleId;
        return new Rule(violation, messageCode, standardRule, List.of());
    }

    private static Rule resolveRule(build.buf.protovalidate.Violation violation) {
        Violation protoViolation = violation.toProto();
        Rule rule = resolveRule(protoViolation);
        List<Object> messageArguments =
                rule.standardRule() ? List.of(violation.getRuleValue().getValue()) : List.of();
        return new Rule(protoViolation, rule.messageCode(), rule.standardRule(), messageArguments);
    }

    private record Rule(
            Violation violation,
            String messageCode,
            boolean standardRule,
            List<Object> messageArguments) {}
}
