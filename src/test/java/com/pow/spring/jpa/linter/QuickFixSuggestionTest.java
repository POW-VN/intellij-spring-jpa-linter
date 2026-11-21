package com.pow.spring.jpa.linter;

import com.pow.spring.jpa.linter.parser.MethodLexer;
import com.pow.spring.jpa.linter.parser.ParsedQueryMethod;
import com.pow.spring.jpa.linter.resolver.EntityInfo;
import com.pow.spring.jpa.linter.resolver.EntityResolver;
import com.pow.spring.jpa.linter.validator.DiagnosticIssue;
import com.pow.spring.jpa.linter.validator.JavaMethodInfo;
import com.pow.spring.jpa.linter.validator.QueryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class QuickFixSuggestionTest {

    private EntityResolver resolver;
    private QueryValidator validator;

    @BeforeEach
    void setUp() {
        resolver = new EntityResolver();
        EntityInfo userEntity = new EntityInfo("User", "com.example.User");
        userEntity.addField("id", "Long");
        userEntity.addField("username", "String");
        userEntity.addField("email", "String");
        userEntity.addField("role", "String");
        userEntity.addField("status", "String");
        userEntity.addField("createdAt", "LocalDateTime");
        userEntity.addField("age", "Integer");
        resolver.registerEntity(userEntity);

        validator = new QueryValidator(resolver);
    }

    @Test
    void testKeywordTypoInBy_countBsy() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("countBsyRoleAndStatus");
        assertThat(parsed.getSuggestedMethodNames()).contains("countByRoleAndStatus");

        JavaMethodInfo method = new JavaMethodInfo("countBsyRoleAndStatus", "long", false, 2);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0).getSuggestions()).contains("countByRoleAndStatus");
    }

    @Test
    void testExtraCharAfterBy_countBys() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("countBysRoleAndStatus");
        assertThat(parsed.getSuggestedMethodNames()).contains("countByRoleAndStatus");

        JavaMethodInfo method = new JavaMethodInfo("countBysRoleAndStatus", "long", false, 2);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0).getSuggestions()).contains("countByRoleAndStatus");
    }

    @Test
    void testMissingBy_findEmail() {
        JavaMethodInfo method = new JavaMethodInfo("findEmail", "User", false, 1);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0).getSuggestions()).contains("findByEmail");
    }

    @Test
    void testPropertyTypo_findByUsernamme() {
        JavaMethodInfo method = new JavaMethodInfo("findByUsernamme", "User", false, 1);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        DiagnosticIssue propIssue = issues.stream()
                .filter(i -> i.getMessage().contains("does not exist on entity"))
                .findFirst().orElseThrow();
        assertThat(propIssue.getSuggestions()).contains("findByUsername");
    }

    @Test
    void testOperatorTypo_findByAgeGraterThan() {
        JavaMethodInfo method = new JavaMethodInfo("findByAgeGraterThan", "List<User>", false, 1);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        DiagnosticIssue opIssue = issues.stream()
                .filter(i -> i.getMessage().contains("Unknown operator"))
                .findFirst().orElseThrow();
        assertThat(opIssue.getSuggestions()).contains("findByAgeGreaterThan");
    }

    @Test
    void testSortTypo_findByStatusOrderrBy() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByStatusOrderrByCreatedAtDesc");
        assertThat(parsed.getSuggestedMethodNames()).contains("findByStatusOrderByCreatedAtDesc");

        JavaMethodInfo method = new JavaMethodInfo("findByStatusOrderrByCreatedAtDesc", "List<User>", false, 1);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0).getSuggestions()).contains("findByStatusOrderByCreatedAtDesc");
    }

    @Test
    void testSortDirectionTypo_findByStatusOrderByCreatedAtDescc() {
        JavaMethodInfo method = new JavaMethodInfo("findByStatusOrderByCreatedAtDescc", "List<User>", false, 1);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "User");
        assertThat(issues).isNotEmpty();
        DiagnosticIssue sortIssue = issues.stream()
                .filter(i -> i.getMessage().contains("Invalid sort direction"))
                .findFirst().orElseThrow();
        assertThat(sortIssue.getSuggestions()).contains("findByStatusOrderByCreatedAtDesc");
    }
}
