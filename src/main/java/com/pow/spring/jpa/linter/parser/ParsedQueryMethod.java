package com.pow.spring.jpa.linter.parser;

import java.util.Collections;
import java.util.List;

public class ParsedQueryMethod {
    private final String originalName;
    private final boolean isDerivedQuery;
    private final String prefixError;
    private final List<String> syntaxErrors;
    private final String subject;
    private final boolean isDistinct;
    private final Integer limit;
    private final List<QueryCondition> conditions;
    private final List<String> conjunctions;
    private final List<QuerySort> sorts;
    private final int totalExpectedParams;
    private final List<String> suggestedMethodNames;

    public ParsedQueryMethod(
            String originalName,
            boolean isDerivedQuery,
            String prefixError,
            List<String> syntaxErrors,
            String subject,
            boolean isDistinct,
            Integer limit,
            List<QueryCondition> conditions,
            List<String> conjunctions,
            List<QuerySort> sorts,
            int totalExpectedParams
    ) {
        this(originalName, isDerivedQuery, prefixError, syntaxErrors, subject, isDistinct, limit, conditions, conjunctions, sorts, totalExpectedParams, Collections.emptyList());
    }

    public ParsedQueryMethod(
            String originalName,
            boolean isDerivedQuery,
            String prefixError,
            List<String> syntaxErrors,
            String subject,
            boolean isDistinct,
            Integer limit,
            List<QueryCondition> conditions,
            List<String> conjunctions,
            List<QuerySort> sorts,
            int totalExpectedParams,
            List<String> suggestedMethodNames
    ) {
        this.originalName = originalName;
        this.isDerivedQuery = isDerivedQuery;
        this.prefixError = prefixError;
        this.syntaxErrors = syntaxErrors != null ? syntaxErrors : Collections.emptyList();
        this.subject = subject;
        this.isDistinct = isDistinct;
        this.limit = limit;
        this.conditions = conditions != null ? conditions : Collections.emptyList();
        this.conjunctions = conjunctions != null ? conjunctions : Collections.emptyList();
        this.sorts = sorts != null ? sorts : Collections.emptyList();
        this.totalExpectedParams = totalExpectedParams;
        this.suggestedMethodNames = suggestedMethodNames != null ? suggestedMethodNames : Collections.emptyList();
    }

    public String getOriginalName() { return originalName; }
    public boolean isDerivedQuery() { return isDerivedQuery; }
    public String getPrefixError() { return prefixError; }
    public List<String> getSyntaxErrors() { return syntaxErrors; }
    public String getSubject() { return subject; }
    public boolean isDistinct() { return isDistinct; }
    public Integer getLimit() { return limit; }
    public List<QueryCondition> getConditions() { return conditions; }
    public List<String> getConjunctions() { return conjunctions; }
    public List<QuerySort> getSorts() { return sorts; }
    public int getTotalExpectedParams() { return totalExpectedParams; }
    public List<String> getSuggestedMethodNames() { return suggestedMethodNames; }
}
