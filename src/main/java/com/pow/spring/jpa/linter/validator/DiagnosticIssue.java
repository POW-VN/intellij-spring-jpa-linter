package com.pow.spring.jpa.linter.validator;

import java.util.List;

public class DiagnosticIssue {
    private final String message;
    private final String severity; // Error, Warning, Information
    private final String methodName;
    private final List<String> suggestions;

    public DiagnosticIssue(String message, String severity, String methodName, List<String> suggestions) {
        this.message = message;
        this.severity = severity;
        this.methodName = methodName;
        this.suggestions = suggestions;
    }

    public String getMessage() { return message; }
    public String getSeverity() { return severity; }
    public String getMethodName() { return methodName; }
    public List<String> getSuggestions() { return suggestions; }
}
