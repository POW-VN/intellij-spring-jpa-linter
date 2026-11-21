package com.pow.spring.jpa.linter;

import com.pow.spring.jpa.linter.parser.MethodLexer;
import com.pow.spring.jpa.linter.parser.ParsedQueryMethod;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LexerTest {

    @Test
    void isDerivedQueryMethod_shouldDetectDerivedQueryPrefixes() {
        assertThat(MethodLexer.isDerivedQueryMethod("findByRoomId")).isTrue();
        assertThat(MethodLexer.isDerivedQueryMethod("countByStatus")).isTrue();
        assertThat(MethodLexer.isDerivedQueryMethod("existsByRoomIdAndBillingMonth")).isTrue();
        assertThat(MethodLexer.isDerivedQueryMethod("deleteByReadingDateBefore")).isTrue();
        assertThat(MethodLexer.isDerivedQueryMethod("findTopByRoomIdOrderByBillingYearDesc")).isTrue();
        assertThat(MethodLexer.isDerivedQueryMethod("customQuery")).isFalse();
        assertThat(MethodLexer.isDerivedQueryMethod("saveEntity")).isFalse();
    }

    @Test
    void parseQueryMethod_parsesSinglePropertyFindBy() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findByRoomId");
        assertThat(parsed.isDerivedQuery()).isTrue();
        assertThat(parsed.getSubject()).isEqualTo("find");
        assertThat(parsed.getConditions()).hasSize(1);
        assertThat(parsed.getConditions().get(0).getPropertyName()).isEqualTo("RoomId");
        assertThat(parsed.getConditions().get(0).getOperator()).isEqualTo("Equals");
        assertThat(parsed.getTotalExpectedParams()).isEqualTo(1);
    }

    @Test
    void parseQueryMethod_parsesTopAndOrderBy() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTopByRoomIdOrderByBillingYearDescBillingMonthDesc");
        assertThat(parsed.isDerivedQuery()).isTrue();
        assertThat(parsed.getSubject()).isEqualTo("find");
        assertThat(parsed.getLimit()).isEqualTo(1);
        assertThat(parsed.getConditions()).hasSize(1);
        assertThat(parsed.getConditions().get(0).getPropertyName()).isEqualTo("RoomId");
        assertThat(parsed.getSorts()).hasSize(2);
        assertThat(parsed.getSorts().get(0).getPropertyName()).isEqualTo("BillingYear");
        assertThat(parsed.getSorts().get(0).getDirection()).isEqualTo("Desc");
        assertThat(parsed.getSorts().get(1).getPropertyName()).isEqualTo("BillingMonth");
        assertThat(parsed.getSorts().get(1).getDirection()).isEqualTo("Desc");
    }

    @Test
    void parseQueryMethod_catchesSortDirectionTypos() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTopByRoomIdOrderByBillingYearDescBillingMonthDescss");
        assertThat(parsed.getSorts()).hasSize(2);
        assertThat(parsed.getSorts().get(1).getDirectionError()).contains("Invalid sort direction 'Descss'. Did you mean 'Desc'?");
    }

    @Test
    void parseQueryMethod_catchesModifierTypos() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTopssByRoomIdOrderByBillingYearDesc");
        assertThat(parsed.getPrefixError()).contains("Topss");
    }

    @Test
    void parseQueryMethod_catchesTypoInByPrefix() {
        assertThat(MethodLexer.isDerivedQueryMethod("countBysRoleAndStatus")).isTrue();
        ParsedQueryMethod parsedBys = MethodLexer.parseQueryMethod("countBysRoleAndStatus");
        assertThat(parsedBys.getSyntaxErrors()).anyMatch(err -> err.contains("Unexpected character 's' after 'By'"));

        assertThat(MethodLexer.isDerivedQueryMethod("countBsyRoleAndStatus")).isTrue();
        ParsedQueryMethod parsedBsy = MethodLexer.parseQueryMethod("countBsyRoleAndStatus");
        assertThat(parsedBsy.getSyntaxErrors()).anyMatch(err -> err.contains("Invalid keyword 'Bsy'. Did you mean 'By'?"));
    }
}
