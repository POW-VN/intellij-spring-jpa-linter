package com.pow.spring.jpa.linter.parser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodLexer {

    public static final List<String> VALID_PREFIX_ROOTS = Collections.unmodifiableList(Arrays.asList(
            "find", "read", "get", "query", "search", "stream", "count", "exists", "delete", "remove"
    ));

    private static final Pattern PREFIX_PATTERN = Pattern.compile("^([a-z]+)(.*)By$");
    private static final Pattern ORDER_TYPO_PATTERN = Pattern.compile("([oO]rder[a-z]*[bB]y[a-z]*|[oO]der[a-z]*[bB]y[a-z]*|[oO]rder[a-z]+)(?=[A-Z0-9_]|$)");
    private static final Pattern DIR_PATTERN = Pattern.compile("(Asc|Desc|[Aa]s[a-z]*|[Dd]es[a-z]*)(?=[A-Z0-9_]|$)");

    public static class OperatorDef {
        public final String name;
        public final int params;
        public final List<String> aliases;

        public OperatorDef(String name, int params, String... aliases) {
            this.name = name;
            this.params = params;
            this.aliases = aliases != null ? Arrays.asList(aliases) : Collections.emptyList();
        }
    }

    public static final List<OperatorDef> KNOWN_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            new OperatorDef("GreaterThanEqual", 1, "AfterEqual"),
            new OperatorDef("LessThanEqual", 1, "BeforeEqual"),
            new OperatorDef("NotContaining", 1, "NotContains", "IsNotContaining"),
            new OperatorDef("IsNotEmpty", 0, "NotEmpty"),
            new OperatorDef("IsNotNull", 0, "NotNull"),
            new OperatorDef("StartingWith", 1, "StartsWith", "IsStartingWith"),
            new OperatorDef("EndingWith", 1, "EndsWith", "IsEndingWith"),
            new OperatorDef("Containing", 1, "Contains", "IsContaining"),
            new OperatorDef("GreaterThan", 1, "After", "IsGreaterThan"),
            new OperatorDef("LessThan", 1, "Before", "IsLessThan"),
            new OperatorDef("Between", 2, "IsBetween"),
            new OperatorDef("NotLike", 1, "IsNotLike"),
            new OperatorDef("NotIn", 1, "IsNotIn"),
            new OperatorDef("IsEmpty", 0, "Empty"),
            new OperatorDef("IsNull", 0, "Null"),
            new OperatorDef("IsTrue", 0, "True"),
            new OperatorDef("IsFalse", 0, "False"),
            new OperatorDef("Equals", 1, "Is", "Equal"),
            new OperatorDef("Like", 1, "IsLike"),
            new OperatorDef("Not", 1, "IsNot"),
            new OperatorDef("In", 1, "IsIn"),
            new OperatorDef("IgnoreCase", 1)
    ));

    public static final Map<String, String> OPERATOR_TYPOS = new HashMap<>();
    static {
        OPERATOR_TYPOS.put("greatthan", "GreaterThan");
        OPERATOR_TYPOS.put("greatethan", "GreaterThan");
        OPERATOR_TYPOS.put("greaterthen", "GreaterThan");
        OPERATOR_TYPOS.put("graterthan", "GreaterThan");
        OPERATOR_TYPOS.put("gratethan", "GreaterThan");
        OPERATOR_TYPOS.put("graterthen", "GreaterThan");
        OPERATOR_TYPOS.put("lessthen", "LessThan");
        OPERATOR_TYPOS.put("lessthan", "LessThan");
        OPERATOR_TYPOS.put("notcontaning", "NotContaining");
        OPERATOR_TYPOS.put("contaning", "Containing");
        OPERATOR_TYPOS.put("contaiing", "Containing");
        OPERATOR_TYPOS.put("contain", "Containing");
        OPERATOR_TYPOS.put("conatins", "Containing");
        OPERATOR_TYPOS.put("startwith", "StartingWith");
        OPERATOR_TYPOS.put("starttingwith", "StartingWith");
        OPERATOR_TYPOS.put("endwith", "EndingWith");
        OPERATOR_TYPOS.put("betweeen", "Between");
        OPERATOR_TYPOS.put("betwen", "Between");
        OPERATOR_TYPOS.put("isnotnul", "IsNotNull");
        OPERATOR_TYPOS.put("isnul", "IsNull");
        OPERATOR_TYPOS.put("notin", "NotIn");
        OPERATOR_TYPOS.put("ignorecases", "IgnoreCase");
        OPERATOR_TYPOS.put("ignorecase", "IgnoreCase");
    }

    private static final Pattern DERIVED_QUERY_PATTERN = Pattern.compile("(?<=[a-z])(B[a-z]{1,2}|By|by|OrderBy)(?=[A-Za-z0-9_]|$)");

    public static boolean isDerivedQueryMethod(String methodName) {
        if (methodName == null) return false;
        if (DERIVED_QUERY_PATTERN.matcher(methodName).find()) {
            return true;
        }
        for (String verb : VALID_PREFIX_ROOTS) {
            if (methodName.startsWith(verb)) {
                return true;
            }
        }
        return false;
    }

    public static ParsedQueryMethod parseQueryMethod(String methodName) {
        return parseQueryMethod(methodName, null);
    }

    public static ParsedQueryMethod parseQueryMethod(String methodName, String entityName) {
        List<String> syntaxErrors = new ArrayList<>();
        List<String> suggestedNames = new ArrayList<>();

        if (methodName == null) {
            return new ParsedQueryMethod(null, false, null, syntaxErrors, null, false, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, suggestedNames);
        }

        Matcher orderTypoMatcher = ORDER_TYPO_PATTERN.matcher(methodName);
        if (orderTypoMatcher.find()) {
            String matched = orderTypoMatcher.group(1);
            if (!matched.equals("OrderBy") && (!matched.startsWith("OrderBy") || Pattern.compile("^OrderBy[a-z]").matcher(matched).find())) {
                syntaxErrors.add("Spring Data JPA: Invalid sort keyword '" + matched + "'. Did you mean 'OrderBy'?");
                suggestedNames.add(methodName.replace(matched, "OrderBy"));
            }
        }

        int byIndex = methodName.indexOf("By");
        int byLength = 2;

        if (byIndex == -1) {
            Matcher byTypoMatcher = Pattern.compile("(?<=[a-z])(B[a-z]y|Bys|B[a-z]{1,2}|by)(?=[A-Z]|$)").matcher(methodName);
            if (byTypoMatcher.find()) {
                byIndex = byTypoMatcher.start(1);
                String matchedKeyword = byTypoMatcher.group(1);
                byLength = matchedKeyword.length();
                syntaxErrors.add("Spring Data JPA: Invalid keyword '" + matchedKeyword + "'. Did you mean 'By'?");
                String fixedByName = methodName.substring(0, byIndex) + "By" + methodName.substring(byIndex + byLength);
                suggestedNames.add(fixedByName);
            } else {
                return new ParsedQueryMethod(methodName, false, null, syntaxErrors, null, false, null, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0, suggestedNames);
            }
        }

        String rawPrefix = methodName.substring(0, byIndex) + "By";
        String remainder = methodName.substring(byIndex + byLength);

        // Check for typo in 'By' like 'countBysRole' -> extra 's'
        if (remainder.length() > 1 && Character.isLowerCase(remainder.charAt(0)) && Character.isUpperCase(remainder.charAt(1))) {
            char extraChar = remainder.charAt(0);
            String fixedExtraCharName = rawPrefix + remainder.substring(1);
            syntaxErrors.add("Spring Data JPA: Unexpected character '" + extraChar + "' after 'By'. Did you mean '" + fixedExtraCharName + "'?");
            suggestedNames.add(fixedExtraCharName);
            remainder = remainder.substring(1);
        }

        String prefixError = null;
        String subject = null;
        boolean isDistinct = false;
        Integer limit = null;

        Matcher prefixMatcher = PREFIX_PATTERN.matcher(rawPrefix);
        if (!prefixMatcher.matches()) {
            prefixError = "Spring Data JPA: Invalid query prefix '" + rawPrefix + "'. Query method prefix should start with lowercase verb like 'findBy', 'countBy', etc.";
        } else {
            String rootVerb = prefixMatcher.group(1);
            String modifier = prefixMatcher.group(2);

            if (!VALID_PREFIX_ROOTS.contains(rootVerb)) {
                String closest = getClosestVerb(rootVerb);
                String modStr = modifier != null ? modifier : "";
                prefixError = "Spring Data JPA: Unknown query verb '" + rootVerb + "'. Did you mean '" + closest + modStr + "By'?";
                suggestedNames.add(closest + modStr + "By" + remainder);
            } else {
                subject = rootVerb;

                if (modifier != null && !modifier.isEmpty()) {
                    // 1. Check for typos of 'All': Aaallll, Alll, Al, Aall, etc.
                    if (!"All".equals(modifier) && (modifier.matches("(?i)^a+l+$") || (levenshtein(modifier.toLowerCase(), "all") <= 1 && modifier.length() <= 4))) {
                        prefixError = "Spring Data JPA: Invalid modifier '" + modifier + "' in prefix '" + rawPrefix + "'. Did you mean 'All'?";
                        suggestedNames.add(rootVerb + "AllBy" + remainder);
                        modifier = "";
                    } else if ("All".equals(modifier)) {
                        modifier = "";
                    } else if (modifier.startsWith("AllDistinct")) {
                        isDistinct = true;
                        modifier = modifier.substring(11);
                    } else if (modifier.startsWith("DistinctAll")) {
                        isDistinct = true;
                        modifier = modifier.substring(11);
                    } else if (modifier.startsWith("All")) {
                        modifier = modifier.substring(3);
                    }

                    if ("Distinct".equals(modifier)) {
                        isDistinct = true;
                        modifier = "";
                    } else if (modifier.startsWith("DistinctTop") || modifier.startsWith("DistinctFirst")) {
                        isDistinct = true;
                        modifier = modifier.substring(8);
                    } else if (Pattern.compile("^dis[a-z]+ct", Pattern.CASE_INSENSITIVE).matcher(modifier).find() || (levenshtein(modifier.toLowerCase(), "distinct") <= 2 && modifier.length() >= 6)) {
                        prefixError = "Spring Data JPA: Invalid keyword '" + modifier + "' in prefix. Did you mean 'Distinct'?";
                        suggestedNames.add(rootVerb + "DistinctBy" + remainder);
                        modifier = "";
                    }

                    Matcher limitMatcher = Pattern.compile("^(Top|First)(\\d+)?$").matcher(modifier);
                    if (limitMatcher.matches()) {
                        String countStr = limitMatcher.group(2);
                        int count = (countStr != null && !countStr.isEmpty()) ? Integer.parseInt(countStr) : 1;
                        if (count == 0) {
                            prefixError = "Spring Data JPA: Limit count in '" + modifier + "' must be greater than 0.";
                            suggestedNames.add(rootVerb + limitMatcher.group(1) + "1By" + remainder);
                        } else {
                            limit = count;
                        }
                        modifier = "";
                    } else if (Pattern.compile("^(Top|First)", Pattern.CASE_INSENSITIVE).matcher(modifier).find()) {
                        String intended = modifier.toLowerCase().startsWith("top") ? "Top" : "First";
                        prefixError = "Spring Data JPA: Invalid limit modifier '" + modifier + "' in prefix '" + rawPrefix + "'. Did you mean '" + intended + "' or '" + intended + "1'?";
                        suggestedNames.add(rootVerb + intended + "By" + remainder);
                        modifier = "";
                    } else if (entityName != null && (modifier.equalsIgnoreCase(entityName) || modifier.equalsIgnoreCase(entityName + "s"))) {
                        // allowed entity descriptor
                        modifier = "";
                    } else if (!modifier.isEmpty()) {
                        Matcher topEntityMatcher = Pattern.compile("^(Top|First)(\\d+)?([A-Z].*)$").matcher(modifier);
                        if (topEntityMatcher.matches()) {
                            String countStr = topEntityMatcher.group(2);
                            int count = (countStr != null && !countStr.isEmpty()) ? Integer.parseInt(countStr) : 1;
                            if (count == 0) {
                                prefixError = "Spring Data JPA: Limit count in '" + modifier + "' must be greater than 0.";
                            } else {
                                limit = count;
                            }
                            modifier = "";
                        } else {
                            if (entityName != null && (levenshtein(modifier.toLowerCase(), entityName.toLowerCase()) <= 2 || levenshtein(modifier.toLowerCase(), (entityName.toLowerCase() + "s")) <= 2)) {
                                prefixError = "Spring Data JPA: Unknown modifier '" + modifier + "' in prefix '" + rawPrefix + "'. Did you mean '" + entityName + "'?";
                                suggestedNames.add(rootVerb + entityName + "By" + remainder);
                            } else {
                                prefixError = "Spring Data JPA: Unrecognized modifier '" + modifier + "' in prefix '" + rawPrefix + "'. Valid modifiers are 'All', 'Distinct', 'Top<N>', 'First<N>'" + (entityName != null ? ", or entity name '" + entityName + "'" : "") + ".";
                            }
                        }
                    }
                }
            }
        }

        // Parse Criteria and Sorts
        String criteriaPart = remainder;
        String orderPart = "";

        if (remainder.isEmpty()) {
            syntaxErrors.add("Spring Data JPA: Query method '" + methodName + "' has an incomplete 'By' clause. Missing criteria or 'OrderBy'.");
        }

        int orderByIndex = remainder.indexOf("OrderBy");
        if (orderByIndex != -1) {
            criteriaPart = remainder.substring(0, orderByIndex);
            orderPart = remainder.substring(orderByIndex + 7);
        }

        List<QuerySort> sorts = parseOrderPart(orderPart);

        // Check conjunction typos (e.g. Andd, Orr)
        Matcher conjTypoMatcher = Pattern.compile("(?<=[a-z])(Andd|Annd|Adn|Orr|Oor)(?=[A-Z])").matcher(criteriaPart);
        if (conjTypoMatcher.find()) {
            String matched = conjTypoMatcher.group(1);
            String correctConj = matched.startsWith("A") ? "And" : "Or";
            syntaxErrors.add("Spring Data JPA: Invalid conjunction '" + matched + "'. Did you mean '" + correctConj + "'?");
            suggestedNames.add(methodName.replace(matched, correctConj));
        }

        List<QueryCondition> conditions = new ArrayList<>();
        List<String> conjunctions = new ArrayList<>();

        if (!criteriaPart.isEmpty()) {
            Pattern splitPattern = Pattern.compile("(And|Or)(?=[A-Z])");
            Matcher m = splitPattern.matcher(criteriaPart);
            int lastEnd = 0;
            while (m.find()) {
                String token = criteriaPart.substring(lastEnd, m.start());
                if (!token.isEmpty()) {
                    conditions.add(parseConditionToken(token));
                }
                conjunctions.add(m.group(1));
                lastEnd = m.end();
            }
            String trailing = criteriaPart.substring(lastEnd);
            if (!trailing.isEmpty()) {
                conditions.add(parseConditionToken(trailing));
            }
        }

        int totalExpectedParams = conditions.stream().mapToInt(QueryCondition::getRequiredParams).sum();
        return new ParsedQueryMethod(
                methodName,
                true,
                prefixError,
                syntaxErrors,
                subject,
                isDistinct,
                limit,
                conditions,
                conjunctions,
                sorts,
                totalExpectedParams,
                suggestedNames
        );
    }

    public static List<QuerySort> parseOrderPart(String orderPart) {
        List<QuerySort> sorts = new ArrayList<>();
        if (orderPart == null || orderPart.isEmpty()) {
            return sorts;
        }

        Matcher matcher = DIR_PATTERN.matcher(orderPart);
        int lastIndex = 0;

        class Chunk {
            String propPart;
            String dirToken;
            Chunk(String p, String d) { this.propPart = p; this.dirToken = d; }
        }

        List<Chunk> chunks = new ArrayList<>();

        while (matcher.find()) {
            String dirToken = matcher.group(1);
            int matchStart = matcher.start();
            String propPart = orderPart.substring(lastIndex, matchStart);
            if (!propPart.isEmpty()) {
                chunks.add(new Chunk(propPart, dirToken));
                lastIndex = matchStart + dirToken.length();
            }
        }

        String leftover = orderPart.substring(lastIndex);
        if (!leftover.isEmpty()) {
            if (!chunks.isEmpty() && !chunks.get(chunks.size() - 1).dirToken.isEmpty()) {
                Chunk lastChunk = chunks.get(chunks.size() - 1);
                lastChunk.dirToken = lastChunk.dirToken + leftover;
            } else {
                chunks.add(new Chunk(leftover, "Asc"));
            }
        }

        if (chunks.isEmpty() && !orderPart.isEmpty()) {
            chunks.add(new Chunk(orderPart, "Asc"));
        }

        for (Chunk chunk : chunks) {
            String propPart = chunk.propPart;
            String dirToken = chunk.dirToken;

            if ("Asc".equals(dirToken) || "Desc".equals(dirToken)) {
                sorts.add(new QuerySort(propPart + dirToken, propPart, dirToken, null));
            } else {
                String resolvedDir = dirToken.toLowerCase().startsWith("as") ? "Asc" : "Desc";
                sorts.add(new QuerySort(
                        propPart + dirToken,
                        propPart,
                        resolvedDir,
                        "Invalid sort direction '" + dirToken + "'. Did you mean '" + resolvedDir + "'?"
                ));
            }
        }

        return sorts;
    }

    private static QueryCondition parseConditionToken(String token) {
        String cleanedToken = token;
        boolean hasIgnoreCase = false;

        if (cleanedToken.endsWith("IgnoreCase")) {
            hasIgnoreCase = true;
            cleanedToken = cleanedToken.substring(0, cleanedToken.length() - 10);
        }

        // 1. Check known operators
        for (OperatorDef op : KNOWN_OPERATORS) {
            if ("IgnoreCase".equals(op.name)) continue;

            if (cleanedToken.endsWith(op.name)) {
                String prop = cleanedToken.substring(0, cleanedToken.length() - op.name.length());
                if (!prop.isEmpty()) {
                    return new QueryCondition(token, prop, op.name, null, op.params);
                }
            }

            for (String alias : op.aliases) {
                if (cleanedToken.endsWith(alias)) {
                    String prop = cleanedToken.substring(0, cleanedToken.length() - alias.length());
                    if (!prop.isEmpty()) {
                        return new QueryCondition(token, prop, op.name, null, op.params);
                    }
                }
            }
        }

        // 2. Check common operator typos
        for (Map.Entry<String, String> entry : OPERATOR_TYPOS.entrySet()) {
            String typo = entry.getKey();
            String correctOp = entry.getValue();

            if (cleanedToken.toLowerCase().endsWith(typo)) {
                String prop = cleanedToken.substring(0, cleanedToken.length() - typo.length());
                if (!prop.isEmpty()) {
                    int params = 1;
                    for (OperatorDef o : KNOWN_OPERATORS) {
                        if (o.name.equals(correctOp)) {
                            params = o.params;
                            break;
                        }
                    }
                    return new QueryCondition(
                            token,
                            prop,
                            correctOp,
                            "Unknown operator in '" + token + "'. Did you mean '" + prop + correctOp + "'?",
                            params
                    );
                }
            }
        }

        return new QueryCondition(
                token,
                cleanedToken,
                hasIgnoreCase ? "IgnoreCase" : "Equals",
                null,
                1
        );
    }

    private static String getClosestVerb(String verb) {
        if (verb.startsWith("find")) return "find";
        if (verb.startsWith("get")) return "get";
        if (verb.startsWith("read")) return "read";
        if (verb.startsWith("count")) return "count";
        if (verb.startsWith("exist")) return "exists";
        if (verb.startsWith("delete")) return "delete";
        return "find";
    }

    public static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) d[i][0] = i;
        for (int j = 0; j <= n; j++) d[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = Character.toLowerCase(a.charAt(i - 1)) == Character.toLowerCase(b.charAt(j - 1)) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[m][n];
    }
}
