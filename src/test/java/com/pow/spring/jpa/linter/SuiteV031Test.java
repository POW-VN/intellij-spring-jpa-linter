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
}
