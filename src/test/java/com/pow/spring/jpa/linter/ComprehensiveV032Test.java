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

public class ComprehensiveV032Test {

    private EntityResolver resolver;
    private QueryValidator validator;

    @BeforeEach
    void setUp() {
        resolver = new EntityResolver();
        EntityInfo invoiceEntity = new EntityInfo("Invoice", "com.rental.manager.entity.tenant.Invoice");
        invoiceEntity.addField("id", "Long");
        invoiceEntity.addField("status", "String");
        invoiceEntity.addField("createdAt", "LocalDateTime");
        invoiceEntity.addField("role", "String");
        resolver.registerEntity(invoiceEntity);

        validator = new QueryValidator(resolver);
    }

    @Test
    void test1_findDistinctInvoiceByStatus_isValidAndDistinct() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findDistinctInvoiceByStatus", "Invoice");
        assertThat(parsed.getPrefixError()).isNull();
        assertThat(parsed.getSyntaxErrors()).isEmpty();
        assertThat(parsed.isDistinct()).isTrue();
        assertThat(parsed.getConditions()).hasSize(1);
        assertThat(parsed.getConditions().get(0).getPropertyName()).isEqualTo("Status");
    }

    @Test
    void test2_findTop3InvoicesByOrderByCreatedAtDesc_isValidAndLimit3() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTop3InvoicesByOrderByCreatedAtDesc", "Invoice");
        assertThat(parsed.getPrefixError()).isNull();
        assertThat(parsed.getSyntaxErrors()).isEmpty();
        assertThat(parsed.getLimit()).isEqualTo(3);
        assertThat(parsed.getSorts()).hasSize(1);
        assertThat(parsed.getSorts().get(0).getPropertyName()).isEqualTo("CreatedAt");
    }

    @Test
    void test3_findByStatusAndRoleAllIgnoreCase_appliesIgnoreCaseToAll() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByStatusAndRoleAllIgnoreCase", "Invoice");
        assertThat(parsed.getPrefixError()).isNull();
        assertThat(parsed.getSyntaxErrors()).isEmpty();
        assertThat(parsed.getConditions()).hasSize(2);
        assertThat(parsed.getConditions().get(0).getPropertyName()).isEqualTo("Status");
        assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo("IgnoreCase");
        assertThat(parsed.getConditions().get(1).getPropertyName()).isEqualTo("Role");
        assertThat(parsed.getConditions().get(1).getOperator()).isEqualTo("IgnoreCase");
    }

    @Test
    void test4_dynamicProjection_ClassParamDoesNotMismatch() {
        JavaMethodInfo method = new JavaMethodInfo("findByStatus", "<T> List<T>", false, 1);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "Invoice");
        assertThat(issues).isEmpty();
    }

    @Test
    void test5_lowercasePropertyName_triggersPascalCaseError() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findBycreatedat", "Invoice");
        assertThat(parsed.getSyntaxErrors()).anyMatch(e -> e.contains("PascalCase"));
        assertThat(parsed.getSuggestedMethodNames()).contains("findByCreatedat");
    }

    @Test
    void test6_trailingConjunction_triggersSyntaxError() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByStatusAnd", "Invoice");
        assertThat(parsed.getSyntaxErrors()).anyMatch(e -> e.contains("trailing conjunction"));
    }

    @Test
    void test7_leadingConjunction_triggersSyntaxError() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByAndStatus", "Invoice");
        assertThat(parsed.getSyntaxErrors()).anyMatch(e -> e.contains("leading conjunction"));
    }

    @Test
    void test8_consecutiveConjunctions_triggerSyntaxError() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByStatusAndOrRole", "Invoice");
        assertThat(parsed.getSyntaxErrors()).anyMatch(e -> e.contains("Consecutive conjunctions"));
    }

    @Test
    void test9_pageReturnWithoutPageable_triggersError() {
        JavaMethodInfo method = new JavaMethodInfo("findByStatus", "Page<Invoice>", false, 1, false);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "Invoice");
        assertThat(issues).anyMatch(i -> i.getMessage().contains("must declare a 'Pageable' parameter"));
    }

    @Test
    void test10_countByNonNumeric_triggersError() {
        JavaMethodInfo method = new JavaMethodInfo("countByStatus", "List<Invoice>", false, 1, false);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "Invoice");
        assertThat(issues).anyMatch(i -> i.getMessage().contains("'countBy' query method must return a numeric type"));
    }

    @Test
    void test11_existsByNonBoolean_triggersError() {
        JavaMethodInfo method = new JavaMethodInfo("existsByStatus", "List<Invoice>", false, 1, false);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "Invoice");
        assertThat(issues).anyMatch(i -> i.getMessage().contains("'existsBy' query method must return 'boolean' or 'Boolean'"));
    }
}
