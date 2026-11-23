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

public class SmartResolverV040Test {

    private EntityResolver resolver;
    private QueryValidator validator;

    @BeforeEach
    void setUp() {
        resolver = new EntityResolver();
        validator = new QueryValidator(resolver);
    }

    @Test
    void test1_smartRelationDetectionAndNestedIdResolution() {
        EntityInfo invoice = new EntityInfo("Invoice", "com.rental.manager.entity.tenant.Invoice");
        invoice.addField("id", "Long");
        invoice.addField("totalAmount", "Double");
        resolver.registerEntity(invoice);

        EntityInfo invoiceItem = new EntityInfo("InvoiceItem", "com.rental.manager.entity.tenant.InvoiceItem");
        invoiceItem.addField("id", "Long");
        // Field type is "Invoice" (entity name) -> smart relation auto-detection
        invoiceItem.addField("invoice", "Invoice");
        invoiceItem.addField("description", "String");
        resolver.registerEntity(invoiceItem);

        // Validate findByInvoiceId
        EntityResolver.PropertyResolution res = resolver.resolvePropertyPath("InvoiceItem", "InvoiceId");
        assertThat(res.valid).isTrue();
        assertThat(res.pathSegments).containsExactly("invoice", "id");

        // Verify JPQL explanation
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByInvoiceId", "InvoiceItem");
        ExplanationResult exp = QueryExplainer.explainQueryMethod(parsed, "InvoiceItem", Collections.singletonList("invoiceId"), "en", resolver);
        assertThat(exp.getSqlPreview()).contains("i.invoice.id = :invoiceId");
    }

    @Test
    void test2_mappedSuperclassInheritance() {
        EntityInfo baseEntity = new EntityInfo("BaseEntity", "com.rental.manager.entity.base.BaseEntity");
        baseEntity.addField("id", "Long");
        baseEntity.addField("createdAt", "LocalDateTime");
        baseEntity.addField("updatedAt", "LocalDateTime");
        resolver.registerEntity(baseEntity);

        EntityInfo room = new EntityInfo("Room", "com.rental.manager.entity.property.Room");
        room.setSuperClassName("BaseEntity");
        room.addField("roomNumber", "String");
        resolver.registerEntity(room);

        // Check inherited properties
        assertThat(resolver.resolvePropertyPath("Room", "Id").valid).isTrue();
        assertThat(resolver.resolvePropertyPath("Room", "CreatedAt").valid).isTrue();
        assertThat(resolver.resolvePropertyPath("Room", "RoomNumber").valid).isTrue();
    }

    @Test
    void test3_deepMultiLevelRecursiveTraversal() {
        EntityInfo building = new EntityInfo("Building", "Building");
        building.addField("id", "Long");
        building.addField("name", "String");
        resolver.registerEntity(building);

        EntityInfo room = new EntityInfo("Room", "Room");
        room.addField("id", "Long");
        room.addField("building", "Building");
        resolver.registerEntity(room);

        EntityInfo contract = new EntityInfo("Contract", "Contract");
        contract.addField("id", "Long");
        contract.addField("room", "Room");
        resolver.registerEntity(contract);

        // 3 levels without underscore: findByRoomBuildingName -> room.building.name
        EntityResolver.PropertyResolution res = resolver.resolvePropertyPath("Contract", "RoomBuildingName");
        assertThat(res.valid).isTrue();
        assertThat(res.pathSegments).containsExactly("room", "building", "name");

        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByRoomBuildingName", "Contract");
        ExplanationResult exp = QueryExplainer.explainQueryMethod(parsed, "Contract", Collections.singletonList("name"), "en", resolver);
        assertThat(exp.getSqlPreview()).contains("c.room.building.name = :name");
    }

    @Test
    void test4_transientPropertyRejected() {
        EntityInfo product = new EntityInfo("Product", "Product");
        product.addField("id", "Long");
        product.addField("temporaryDiscount", "Double");
        product.addTransientField("temporaryDiscount");
        resolver.registerEntity(product);

        EntityResolver.PropertyResolution res = resolver.resolvePropertyPath("Product", "TemporaryDiscount");
        assertThat(res.valid).isFalse();
        assertThat(res.isTransient).isTrue();
        assertThat(res.error).contains("@Transient");
    }

    @Test
    void test5_validateRepositoryMethodProducesZeroErrors() {
        EntityInfo invoice = new EntityInfo("Invoice", "Invoice");
        invoice.addField("id", "Long");
        resolver.registerEntity(invoice);

        EntityInfo invoiceItem = new EntityInfo("InvoiceItem", "InvoiceItem");
        invoiceItem.addField("id", "Long");
        invoiceItem.addField("invoice", "Invoice");
        resolver.registerEntity(invoiceItem);

        JavaMethodInfo method = new JavaMethodInfo("findByInvoiceId", "List<InvoiceItem>", false, 1, false);
        List<DiagnosticIssue> issues = validator.validateMethod(method, "InvoiceItem");
        assertThat(issues).isEmpty();
    }
}
