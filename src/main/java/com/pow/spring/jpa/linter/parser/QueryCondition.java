package com.pow.spring.jpa.linter.parser;

public class QueryCondition {
    private final String raw;
    private final String propertyName;
    private final String operator;
    private final String operatorError;
    private final int requiredParams;

    public QueryCondition(String raw, String propertyName, String operator, String operatorError, int requiredParams) {
        this.raw = raw;
        this.propertyName = propertyName;
        this.operator = operator;
        this.operatorError = operatorError;
        this.requiredParams = requiredParams;
    }

    public String getRaw() { return raw; }
    public String getPropertyName() { return propertyName; }
    public String getOperator() { return operator; }
    public String getOperatorError() { return operatorError; }
    public int getRequiredParams() { return requiredParams; }
}
