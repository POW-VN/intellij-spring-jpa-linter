package com.pow.spring.jpa.linter;

import com.pow.spring.jpa.linter.explainer.ExplanationResult;
import com.pow.spring.jpa.linter.explainer.QueryExplainer;
import com.pow.spring.jpa.linter.parser.MethodLexer;
import com.pow.spring.jpa.linter.parser.ParsedQueryMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExplainerTest {

    @Test
    void explainQueryMethod_explainsSimpleFindByInEnglish() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByRoomId");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "UtilityReading", List.of("roomId"), "en");

        assertThat(res.getSqlPreview()).contains("SELECT u");
        assertThat(res.getSqlPreview()).contains("FROM UtilityReading u");
        assertThat(res.getSqlPreview()).contains("WHERE u.roomId = :roomId");
        assertThat(res.getSummary()).isEqualTo("Find `UtilityReading` records");
    }

    @Test
    void explainQueryMethod_explainsComplexFindTopWithOrderBy() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTopByRoomIdOrderByBillingYearDescBillingMonthDesc");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "UtilityReading", List.of("roomId"), "en");

        assertThat(res.getSqlPreview()).contains("WHERE u.roomId = :roomId");
        assertThat(res.getSqlPreview()).contains("ORDER BY u.billingYear DESC, u.billingMonth DESC");
        assertThat(res.getSqlPreview()).contains("LIMIT 1");
        assertThat(res.getSummary()).isEqualTo("Find the first `UtilityReading` record");
    }

    @Test
    void explainQueryMethod_explainsCountByQuery() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("countByStatus");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "UtilityReading", List.of("status"), "en");

        assertThat(res.getSqlPreview()).contains("SELECT COUNT(u)");
        assertThat(res.getSqlPreview()).contains("WHERE u.status = :status");
        assertThat(res.getSummary()).isEqualTo("Count the number of `UtilityReading` records");
    }

    @Test
    void explainQueryMethod_explainsBetweenOperator() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByReadingDateBetween");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "UtilityReading", List.of("startDate", "endDate"), "en");

        assertThat(res.getSqlPreview()).contains("u.readingDate BETWEEN :startDate AND :endDate");
    }

    @Test
    void explainQueryMethod_supportsVietnameseWhenRequested() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByRoomId");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "UtilityReading", List.of("roomId"), "vi");

        assertThat(res.getSummary()).isEqualTo("Tìm kiếm các bản ghi `UtilityReading`");
        assertThat(res.getMarkdownContent()).contains("Mục đích: Tìm kiếm các bản ghi `UtilityReading`");
    }

    @Test
    void explainQueryMethod_explainsExistsByEmailWithHtmlContent() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("existsByEmail");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "User", List.of("email"), "en");

        assertThat(res.getSqlPreview()).contains("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END");
        assertThat(res.getSqlPreview()).contains("FROM User u");
        assertThat(res.getSqlPreview()).contains("WHERE u.email = :email");
        assertThat(res.getSummary()).isEqualTo("Check if any `User` record exists");
        assertThat(res.getHtmlContent()).contains("Spring Data JPA Query");
        assertThat(res.getHtmlContent()).contains("Criteria:");
        assertThat(res.getHtmlContent()).contains("email = :email");
    }

    @Test
    void explainQueryMethod_explainsFindByRoomIdAndStatus() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByRoomIdAndStatus");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "Contract", List.of("roomId", "status"), "en");

        assertThat(res.getSqlPreview()).contains("SELECT c");
        assertThat(res.getSqlPreview()).contains("FROM Contract c");
        assertThat(res.getSqlPreview()).contains("WHERE c.roomId = :roomId AND c.status = :status");
        assertThat(res.getHtmlContent()).contains("Criteria:");
        assertThat(res.getHtmlContent()).contains("<b>AND</b>");
    }
}
