package com.pow.spring.jpa.linter.validator;

import com.pow.spring.jpa.linter.parser.*;
import com.pow.spring.jpa.linter.resolver.EntityResolver;

import java.util.*;

public class QueryValidator {

    private final EntityResolver entityResolver;

    private static final Set<String> BUILTIN_REPOSITORY_METHODS = Set.of(
            "save", "saveall", "saveandflush", "saveallandflush",
            "findbyid", "existsbyid", "findall", "findallbyid", "count",
            "deletebyid", "delete", "deleteallbyid", "deleteall", "deleteinbatch", "deleteallinbatch",
            "flush", "getone", "getbyid", "getreferencebyid"
    );

    public static boolean isBuiltinMethod(String name) {
        return name != null && BUILTIN_REPOSITORY_METHODS.contains(name.toLowerCase());
    }

    public QueryValidator(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public List<DiagnosticIssue> validateMethod(JavaMethodInfo method, String entityName) {
        List<DiagnosticIssue> issues = new ArrayList<>();

        if (method.isHasQueryAnnotation()) {
            return issues;
        }

        String name = method.getName();
        if (BUILTIN_REPOSITORY_METHODS.contains(name.toLowerCase())) {
            return issues;
        }

        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod(name, entityName);

        // Check if starts with query verb but misses 'By'
        if (!parsed.isDerivedQuery()) {
            for (String root : MethodLexer.VALID_PREFIX_ROOTS) {
                if (name.startsWith(root) && name.length() > root.length() && Character.isUpperCase(name.charAt(root.length()))) {
                    issues.add(new DiagnosticIssue(
                            "Spring Data JPA: Method name starts with '" + root + "' but is missing 'By'. Did you mean '" + root + "By" + name.substring(root.length()) + "'?",
                            "Warning",
                            name,
                            Collections.singletonList(root + "By" + name.substring(root.length()))
                    ));
                    break;
                }
            }
            return issues;
        }

        // 1. Syntax errors
        for (String err : parsed.getSyntaxErrors()) {
            issues.add(new DiagnosticIssue(err, "Error", name, parsed.getSuggestedMethodNames()));
        }

        // 2. Prefix errors
        if (parsed.getPrefixError() != null) {
            issues.add(new DiagnosticIssue(parsed.getPrefixError(), "Error", name, parsed.getSuggestedMethodNames()));
        }

        // 3. Conditions & properties
        for (QueryCondition cond : parsed.getConditions()) {
            if (cond.getOperatorError() != null) {
                String fixedName = name.replace(cond.getRaw(), cond.getPropertyName() + (cond.getOperator() != null ? cond.getOperator() : ""));
                issues.add(new DiagnosticIssue(
                        "Spring Data JPA: " + cond.getOperatorError(),
                        "Error",
                        name,
                        Collections.singletonList(fixedName)
                ));
            }

            if (entityName != null) {
                EntityResolver.PropertyResolution res = entityResolver.resolvePropertyPath(entityName, cond.getPropertyName());
                if (!res.valid) {
                    List<String> suggestions = new ArrayList<>();
                    if (res.suggestion != null) {
                        String fixedProp = fixPropertyName(cond.getPropertyName(), res.suggestion);
                        suggestions.add(name.replace(cond.getPropertyName(), fixedProp));
                    }
                    issues.add(new DiagnosticIssue(
                            "Spring Data JPA: " + res.error,
                            "Error",
                            name,
                            suggestions
                    ));
                }
            }
        }

        // 4. Sorts
        for (QuerySort sort : parsed.getSorts()) {
            if (sort.getDirectionError() != null) {
                String fixedName = name.replace(sort.getRaw(), sort.getPropertyName() + sort.getDirection());
                issues.add(new DiagnosticIssue(
                        "Spring Data JPA: " + sort.getDirectionError(),
                        "Error",
                        name,
                        Collections.singletonList(fixedName)
                ));
            }

            if (entityName != null && sort.getPropertyName() != null && !sort.getPropertyName().isEmpty()) {
                EntityResolver.PropertyResolution res = entityResolver.resolvePropertyPath(entityName, sort.getPropertyName());
                if (!res.valid) {
                    List<String> suggestions = new ArrayList<>();
                    if (res.suggestion != null) {
                        String fixedProp = fixPropertyName(sort.getPropertyName(), res.suggestion);
                        suggestions.add(name.replace(sort.getPropertyName(), fixedProp));
                    }
                    issues.add(new DiagnosticIssue(
                            "Spring Data JPA (Sort): " + res.error,
                            "Error",
                            name,
                            suggestions
                    ));
                }
            }
        }

        // 5. Parameter count validation
        int expectedParams = parsed.getTotalExpectedParams();
        int actualParams = method.getParameterCount();

        if (actualParams != expectedParams && (parsed.getPrefixError() == null && parsed.getSyntaxErrors().isEmpty())) {
            issues.add(new DiagnosticIssue(
                    "Spring Data JPA: Parameter count mismatch. Query method expects " + expectedParams + " parameter(s) for conditions, but got " + actualParams + ".",
                    "Warning",
                    name,
                    Collections.emptyList()
            ));
        }

        // 6. Return type and signature validation
        String retType = method.getReturnType() != null ? method.getReturnType().trim() : "";
        if ((retType.startsWith("Page") || retType.startsWith("Slice")) && !method.isHasPageable()) {
            issues.add(new DiagnosticIssue(
                    "Spring Data JPA: Method returning '" + (retType.startsWith("Page") ? "Page" : "Slice") + "' must declare a 'Pageable' parameter.",
                    "Error",
                    name,
                    Collections.emptyList()
            ));
        }

        if (("count".equals(parsed.getSubject()) || name.startsWith("count")) && !Arrays.asList("long", "Long", "int", "Integer", "Object").contains(retType)) {
            issues.add(new DiagnosticIssue(
                    "Spring Data JPA: 'countBy' query method must return a numeric type (long, Long, int, Integer), but found '" + retType + "'.",
                    "Error",
                    name,
                    Collections.emptyList()
            ));
        }

        if (("exists".equals(parsed.getSubject()) || name.startsWith("exists")) && !Arrays.asList("boolean", "Boolean", "Object").contains(retType)) {
            issues.add(new DiagnosticIssue(
                    "Spring Data JPA: 'existsBy' query method must return 'boolean' or 'Boolean', but found '" + retType + "'.",
                    "Error",
                    name,
                    Collections.emptyList()
            ));
        }

        return issues;
    }

    private static String fixPropertyName(String rawProp, String suggestion) {
        if (suggestion == null || suggestion.isEmpty()) return rawProp;
        if (rawProp.contains("_")) {
            String[] parts = rawProp.split("_");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append("_");
                if (parts[i].equalsIgnoreCase(suggestion) || parts[i].toLowerCase().contains(suggestion.toLowerCase())) {
                    sb.append(capitalize(suggestion));
                } else {
                    sb.append(parts[i]);
                }
            }
            return sb.toString();
        }
        return capitalize(suggestion);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
