package com.pow.spring.jpa.linter;

import com.pow.spring.jpa.linter.explainer.ExplanationResult;
import com.pow.spring.jpa.linter.explainer.QueryExplainer;
import com.pow.spring.jpa.linter.parser.MethodLexer;
import com.pow.spring.jpa.linter.parser.ParsedQueryMethod;
import com.pow.spring.jpa.linter.resolver.EntityInfo;
import com.pow.spring.jpa.linter.resolver.EntityResolver;
import com.pow.spring.jpa.linter.validator.DiagnosticIssue;
import com.pow.spring.jpa.linter.validator.JavaMethodInfo;
import com.pow.spring.jpa.linter.validator.QueryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SuiteV031Test {

    private EntityResolver resolver;
    private QueryValidator validator;

    @BeforeEach
    void setUp() {
        resolver = new EntityResolver();
        EntityInfo invoiceEntity = new EntityInfo("Invoice", "com.rental.manager.entity.Invoice");
        invoiceEntity.addField("id", "Long");
        invoiceEntity.addField("status", "String");
        invoiceEntity.addField("createdAt", "LocalDateTime");
        invoiceEntity.addField("roles", "List<String>");
        resolver.registerEntity(invoiceEntity);

        validator = new QueryValidator(resolver);
    }

    @Test
    void testFindAllByOrderByCreatedAtDesc_producesZeroErrors() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findAllByOrderByCreatedAtDesc", "Invoice");
        assertThat(parsed.isDerivedQuery()).isTrue();
        assertThat(parsed.getPrefixError()).isNull();
        assertThat(parsed.getSyntaxErrors()).isEmpty();

        JavaMethodInfo method = new JavaMethodInfo("findAllByOrderByCreatedAtDesc", "List<Invoice>", false, 0);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "Invoice");
        assertThat(issues).isEmpty();

        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "Invoice", Collections.emptyList(), "en");
        assertThat(res.getSqlPreview()).contains("ORDER BY i.createdAt DESC");
    }

    @Test
    void testFindAllDistinctByStatus_and_findDistinctAllByStatus() {
        ParsedQueryMethod p1 = MethodLexer.parseQueryMethod("findAllDistinctByStatus", "Invoice");
        assertThat(p1.isDerivedQuery()).isTrue();
        assertThat(p1.isDistinct()).isTrue();
        assertThat(p1.getPrefixError()).isNull();

        ParsedQueryMethod p2 = MethodLexer.parseQueryMethod("findDistinctAllByStatus", "Invoice");
        assertThat(p2.isDerivedQuery()).isTrue();
        assertThat(p2.isDistinct()).isTrue();
        assertThat(p2.getPrefixError()).isNull();
    }

    @Test
    void testDescriptiveSubject_findAllInvoicesByStatus() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findAllInvoicesByStatus", "Invoice");
        assertThat(parsed.isDerivedQuery()).isTrue();
        assertThat(parsed.getPrefixError()).isNull();
    }

    @Test
    void testIsEmpty_and_IsNotEmpty() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByRolesIsEmpty", "Invoice");
        assertThat(parsed.isDerivedQuery()).isTrue();
        assertThat(parsed.getConditions()).hasSize(1);
        assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo("IsEmpty");
        assertThat(parsed.getConditions().get(0).getRequiredParams()).isEqualTo(0);

        ParsedQueryMethod parsed2 = MethodLexer.parseQueryMethod("findByRolesIsNotEmpty", "Invoice");
        assertThat(parsed2.getConditions().get(0).getOperator()).isEqualTo("IsNotEmpty");
        assertThat(parsed2.getConditions().get(0).getRequiredParams()).isEqualTo(0);

        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "Invoice", Collections.emptyList(), "en");
        assertThat(res.getSqlPreview()).contains("IS EMPTY");
    }

    @Test
    void testFindAaallllByOrderByCreatedAtDesc_catchesTypoAndSuggestsFindAllBy() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findAaallllByOrderByCreatedAtDesc", "Invoice");
        assertThat(parsed.isDerivedQuery()).isTrue();
        assertThat(parsed.getPrefixError()).contains("Invalid modifier 'Aaallll'");
        assertThat(parsed.getPrefixError()).contains("Did you mean 'All'?");
        assertThat(parsed.getSuggestedMethodNames()).contains("findAllByOrderByCreatedAtDesc");

        JavaMethodInfo method = new JavaMethodInfo("findAaallllByOrderByCreatedAtDesc", "List<Invoice>", false, 0);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "Invoice");
        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0).getMessage()).contains("Did you mean 'All'?");
    }

    @Test
    void testFindAlllByStatus_suggestsFindAllByStatus() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findAlllByStatus", "Invoice");
        assertThat(parsed.getPrefixError()).contains("Did you mean 'All'?");
        assertThat(parsed.getSuggestedMethodNames()).contains("findAllByStatus");
    }

    @Test
    void testFindDistnctByStatus_suggestsFindDistinctByStatus() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findDistnctByStatus", "Invoice");
        assertThat(parsed.getPrefixError()).contains("Did you mean 'Distinct'?");
        assertThat(parsed.getSuggestedMethodNames()).contains("findDistinctByStatus");
    }

    @Test
    void testFindTop0ByStatus_flagsLimitGreaterThanZero() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTop0ByStatus", "Invoice");
        assertThat(parsed.getPrefixError()).contains("Limit count in 'Top0' must be greater than 0");
        assertThat(parsed.getSuggestedMethodNames()).contains("findTop1ByStatus");
    }

    @Test
    void testFindByStatusAnddRole_catchesConjunctionTypo() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByStatusAnddRole", "Invoice");
        assertThat(parsed.getSyntaxErrors()).isNotEmpty();
        assertThat(parsed.getSyntaxErrors().get(0)).contains("Invalid conjunction 'Andd'");
        assertThat(parsed.getSuggestedMethodNames()).contains("findByStatusAndRole");
    }

    @Test
    void testIncompleteByClause() {
        ParsedQueryMethod p1 = MethodLexer.parseQueryMethod("findBy", "Invoice");
        assertThat(p1.getSyntaxErrors()).isNotEmpty();
        assertThat(p1.getSyntaxErrors().get(0)).contains("incomplete 'By' clause");

        ParsedQueryMethod p2 = MethodLexer.parseQueryMethod("findAllBy", "Invoice");
        assertThat(p2.getSyntaxErrors()).isNotEmpty();
        assertThat(p2.getSyntaxErrors().get(0)).contains("incomplete 'By' clause");
    }

    @Test
    void testUnrecognizedModifier_flagsError() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findFoobarByStatus", "Invoice");
        assertThat(parsed.getPrefixError()).contains("Unrecognized modifier 'Foobar'");
    }
}
