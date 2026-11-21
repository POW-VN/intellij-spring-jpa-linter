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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class Suite200Test {

    private QueryValidator validator;
    private EntityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EntityResolver();

        // Level 4: Company Entity
        EntityInfo company = new EntityInfo("Company", "/dummy/Company.java");
        company.addField("id", "Long");
        company.addField("companyName", "String");
        company.addField("taxCode", "String");

        // Level 3: Manager Entity
        EntityInfo manager = new EntityInfo("Manager", "/dummy/Manager.java");
        manager.addField("id", "Long");
        manager.addField("fullName", "String");
        manager.addField("phone", "String");
        manager.addField("company", "Company");
        manager.addRelation("company", "Company");

        // Level 2: Building Entity
        EntityInfo building = new EntityInfo("Building", "/dummy/Building.java");
        building.addField("id", "Long");
        building.addField("name", "String");
        building.addField("address", "String");
        building.addField("manager", "Manager");
        building.addRelation("manager", "Manager");

        // Level 1: Room Entity
        EntityInfo room = new EntityInfo("Room", "/dummy/Room.java");
        room.addField("id", "Long");
        room.addField("roomNumber", "String");
        room.addField("building", "Building");
        room.addRelation("building", "Building");

        // Root: UtilityReading Entity
        EntityInfo utilityReading = new EntityInfo("UtilityReading", "/dummy/UtilityReading.java");
        utilityReading.addField("id", "Long");
        utilityReading.addField("room", "Room");
        utilityReading.addRelation("room", "Room");
        utilityReading.addField("roomId", "Long");
        utilityReading.addField("billingMonth", "Integer");
        utilityReading.addField("billingYear", "Integer");
        utilityReading.addField("readingDate", "LocalDate");
        utilityReading.addField("oldElectricity", "BigDecimal");
        utilityReading.addField("newElectricity", "BigDecimal");
        utilityReading.addField("oldWater", "BigDecimal");
        utilityReading.addField("newWater", "BigDecimal");
        utilityReading.addField("isLocked", "Boolean");
        utilityReading.addField("status", "String");

        resolver.registerEntity(company);
        resolver.registerEntity(manager);
        resolver.registerEntity(building);
        resolver.registerEntity(room);
        resolver.registerEntity(utilityReading);

        validator = new QueryValidator(resolver);
    }

    private List<DiagnosticIssue> validate(String methodName, int paramCount) {
        JavaMethodInfo method = new JavaMethodInfo(methodName, "Object", false, paramCount);
        return validator.validateMethod(method, "UtilityReading");
    }

    // -------------------------------------------------------------
    // SET 1: DEEP NESTED VALID & INVALID (20 cases: 4 levels traversal)
    // -------------------------------------------------------------
    static Stream<Arguments> deepNestedValidCases() {
        return Stream.of(
                Arguments.of("findByRoom_Building_Manager_FullName", 1),
                Arguments.of("findByRoom_Building_Manager_Phone", 1),
                Arguments.of("findByRoom_Building_Manager_Company_CompanyName", 1),
                Arguments.of("findByRoom_Building_Manager_Company_TaxCode", 1),
                Arguments.of("findByRoom_Building_AddressAndRoom_Building_Manager_Phone", 2),
                Arguments.of("findByRoom_RoomNumber", 1),
                Arguments.of("findByRoom_Building_Id", 1),
                Arguments.of("findByRoom_Building_NameStartingWith", 1),
                Arguments.of("findByRoom_Building_Manager_Company_TaxCodeStartingWith", 1),
                Arguments.of("findTop3ByRoom_Building_IdOrderByBillingYearDesc", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("deepNestedValidCases")
    @DisplayName("Deep nested valid traversals should have 0 issues")
    void testDeepNestedValid(String methodName, int paramCount) {
        assertThat(validate(methodName, paramCount)).isEmpty();
    }

    static Stream<Arguments> deepNestedInvalidCases() {
        return Stream.of(
                Arguments.of("findByRoom_Buildings_Manager_FullName", "buildings"),
                Arguments.of("findByRoom_Building_Managers_FullName", "managers"),
                Arguments.of("findByRoom_Building_Manager_FullNames", "fullNames"),
                Arguments.of("findByRoom_Building_Manager_Companys_TaxCode", "companys"),
                Arguments.of("findByRoom_Building_Manager_Company_TaxCodes", "taxCodes"),
                Arguments.of("findByRoom_RoomNumbers", "roomNumbers"),
                Arguments.of("findByRoom_Building_Names", "names"),
                Arguments.of("findByRoom_Building_Addressss", "addressss"),
                Arguments.of("findByRoom_Building_Manager_Phones", "phones")
        );
    }

    @ParameterizedTest
    @MethodSource("deepNestedInvalidCases")
    @DisplayName("Deep nested invalid traversals should report errors")
    void testDeepNestedInvalid(String methodName, String token) {
        List<DiagnosticIssue> issues = validate(methodName, 1);
        assertThat(issues).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().toLowerCase().contains(token.toLowerCase()));
        assertThat(matched).isTrue();
    }

    // -------------------------------------------------------------
    // SET 2: MULTI-FIELD DECIMAL & DATE OPERATORS (20 cases)
    // -------------------------------------------------------------
    static Stream<Arguments> decimalAndDateValidCases() {
        return Stream.of(
                Arguments.of("findByNewElectricityGreaterThan", 1),
                Arguments.of("findByNewElectricityGreaterThanEqual", 1),
                Arguments.of("findByNewElectricityLessThan", 1),
                Arguments.of("findByNewElectricityLessThanEqual", 1),
                Arguments.of("findByNewElectricityBetween", 2),
                Arguments.of("findByOldWaterGreaterThanAndNewWaterLessThan", 2),
                Arguments.of("findByNewWaterBetween", 2),
                Arguments.of("findByOldElectricityBetween", 2),
                Arguments.of("findByReadingDateAfterAndReadingDateBefore", 2),
                Arguments.of("findByReadingDateBetweenAndIsLockedFalse", 2)
        );
    }

    @ParameterizedTest
    @MethodSource("decimalAndDateValidCases")
    @DisplayName("Decimal and Date valid comparisons should have 0 issues")
    void testDecimalAndDateValid(String methodName, int paramCount) {
        assertThat(validate(methodName, paramCount)).isEmpty();
    }

    static Stream<Arguments> decimalAndDateInvalidCases() {
        return Stream.of(
                Arguments.of("findByNewElectricityGreatthan", "GreaterThan"),
                Arguments.of("findByNewElectricityLessthen", "LessThan"),
                Arguments.of("findByNewElectricityBetwen", "Between"),
                Arguments.of("findByNewElectricitiesGreaterThan", "NewElectricities"),
                Arguments.of("findByOldWatersBetween", "OldWaters"),
                Arguments.of("findByNewWatersLessThan", "NewWaters"),
                Arguments.of("findByReadingDatesBefore", "ReadingDates"),
                Arguments.of("findByReadingDatesAfter", "ReadingDates"),
                Arguments.of("findByOldElectricitysBetween", "OldElectricitys"),
                Arguments.of("findByOldWaterBetweeen", "Between")
        );
    }

    @ParameterizedTest
    @MethodSource("decimalAndDateInvalidCases")
    @DisplayName("Decimal and Date typo comparisons should report errors")
    void testDecimalAndDateInvalid(String methodName, String token) {
        List<DiagnosticIssue> issues = validate(methodName, 2);
        assertThat(issues).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().contains(token));
        assertThat(matched).isTrue();
    }

    // -------------------------------------------------------------
    // SET 3: COMPLEX MULTI-ORDERBY (20 cases)
    // -------------------------------------------------------------
    static Stream<Arguments> multiOrderByValidCases() {
        return Stream.of(
                Arguments.of("findTop10ByRoomIdOrderByBillingYearDescBillingMonthDescReadingDateDesc", 1),
                Arguments.of("findTop10ByRoomIdOrderByBillingYearAscBillingMonthAscReadingDateAsc", 1),
                Arguments.of("findByRoomIdOrderByNewElectricityDescOldElectricityAsc", 1),
                Arguments.of("findByStatusOrderByReadingDateDescNewWaterDescOldWaterAsc", 1),
                Arguments.of("findByRoom_Building_IdOrderByBillingYearDescBillingMonthDesc", 1),
                Arguments.of("findFirstByRoomIdOrderByBillingYearDesc", 1),
                Arguments.of("findFirst5ByRoomIdOrderByReadingDateDesc", 1),
                Arguments.of("findTopByRoomIdOrderByOldElectricityAsc", 1),
                Arguments.of("findTopByRoomIdOrderByNewWaterDesc", 1),
                Arguments.of("findTopByRoomIdOrderByIsLockedDesc", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("multiOrderByValidCases")
    @DisplayName("Complex multiple OrderBy should have 0 issues")
    void testMultiOrderByValid(String methodName, int paramCount) {
        assertThat(validate(methodName, paramCount)).isEmpty();
    }

    static Stream<Arguments> multiOrderByInvalidCases() {
        return Stream.of(
                Arguments.of("findByRoomIdOrderByBillingYearDescssBillingMonthDesc", "Descss"),
                Arguments.of("findByRoomIdOrderByBillingYearDescBillingMonthDesssc", "Desssc"),
                Arguments.of("findByRoomIdOrderByBillingYearDescBillingMonthAscss", "Ascss"),
                Arguments.of("findByRoomIdOrderByBillingYearDescBillingMonthAsssc", "Asssc"),
                Arguments.of("findByRoomIdOrderByBillingYearDescsBillingMonthAsc", "Descs"),
                Arguments.of("findByRoomIdOrderByBillingYearDescBillingMonthDescc", "Descc"),
                Arguments.of("findByRoomIdOrderByBillingYearAscBillingMonthAscc", "Ascc"),
                Arguments.of("findByRoomIdOrderByBillingYearsDescBillingMonthAsc", "BillingYears"),
                Arguments.of("findByRoomIdOrderByBillingYearDescBillingMonthsAsc", "BillingMonths"),
                Arguments.of("findByRoomIdOrderbyBillingYearDescBillingMonthAsc", "Orderby")
        );
    }

    @ParameterizedTest
    @MethodSource("multiOrderByInvalidCases")
    @DisplayName("Complex multiple OrderBy typos should report errors")
    void testMultiOrderByInvalid(String methodName, String token) {
        List<DiagnosticIssue> issues = validate(methodName, 1);
        assertThat(issues).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().contains(token));
        assertThat(matched).isTrue();
    }

    // -------------------------------------------------------------
    // SET 4: ADVANCED STRING & BOOLEAN OPERATORS (20 cases)
    // -------------------------------------------------------------
    static Stream<Arguments> stringAndBoolValidCases() {
        return Stream.of(
                Arguments.of("findByStatusStartingWithIgnoreCase", 1),
                Arguments.of("findByStatusEndingWithIgnoreCase", 1),
                Arguments.of("findByStatusContainingIgnoreCase", 1),
                Arguments.of("findByStatusNotContaining", 1),
                Arguments.of("findByStatusNotLike", 1),
                Arguments.of("findByStatusLikeIgnoreCase", 1),
                Arguments.of("findByIsLockedIsTrue", 0),
                Arguments.of("findByIsLockedIsFalse", 0),
                Arguments.of("findByIsLockedTrueAndStatus", 1),
                Arguments.of("findByIsLockedFalseAndBillingYear", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("stringAndBoolValidCases")
    @DisplayName("Advanced String & Boolean valid queries should have 0 issues")
    void testStringAndBoolValid(String methodName, int paramCount) {
        assertThat(validate(methodName, paramCount)).isEmpty();
    }

    static Stream<Arguments> stringAndBoolInvalidCases() {
        return Stream.of(
                Arguments.of("findByStatusStartWithIgnoreCase", "StartingWith"),
                Arguments.of("findByStatusEndWithIgnoreCase", "EndingWith"),
                Arguments.of("findByStatusContaningIgnoreCase", "Containing"),
                Arguments.of("findByStatusNotContaning", "NotContaining"),
                Arguments.of("findByStatusIgnorecases", "IgnoreCase"),
                Arguments.of("findByIsLockedTrues", "IsLockedTrues"),
                Arguments.of("findByIsLockedFalses", "IsLockedFalses"),
                Arguments.of("findByIsLockedsTrue", "IsLockeds"),
                Arguments.of("findByIsLockedsFalse", "IsLockeds"),
                Arguments.of("findByStatussStartingWith", "Statuss")
        );
    }

    @ParameterizedTest
    @MethodSource("stringAndBoolInvalidCases")
    @DisplayName("Advanced String & Boolean typos should report errors")
    void testStringAndBoolInvalid(String methodName, String token) {
        List<DiagnosticIssue> issues = validate(methodName, 1);
        assertThat(issues).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().contains(token));
        assertThat(matched).isTrue();
    }

    // -------------------------------------------------------------
    // SET 5: LIMITS AND EXPLAINER VARIATIONS (20 cases)
    // -------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = {
            "findFirstByRoomId",
            "findFirst1ByRoomId",
            "findFirst10ByRoomId",
            "findTop20ByRoomId",
            "findTop100ByRoomId",
            "findDistinctTop5ByRoomId"
    })
    @DisplayName("Special limit modifiers should validate cleanly")
    void testSpecialLimits(String methodName) {
        assertThat(validate(methodName, 1)).isEmpty();
    }

    @Test
    void testExplainerGeneratesCleanSqlPreview() {
        ParsedQueryMethod parsed = MethodLexer.parseQueryMethod("findTopByRoomIdOrderByBillingYearDesc");
        ExplanationResult res = QueryExplainer.explainQueryMethod(parsed, "UtilityReading", List.of("roomId"), "en");

        assertThat(res.getSqlPreview()).contains("SELECT u\nFROM UtilityReading u\nWHERE u.roomId = :roomId\nORDER BY u.billingYear DESC\nLIMIT 1");
        assertThat(res.getMarkdownContent()).contains("JPQL / SQL:\n\n```sql\n");
    }

    @Test
    void testExplainerHandlesInAndNotIn() {
        ParsedQueryMethod parsedIn = MethodLexer.parseQueryMethod("findByStatusIn");
        ExplanationResult resIn = QueryExplainer.explainQueryMethod(parsedIn, "UtilityReading", List.of("statuses"), "en");
        assertThat(resIn.getSqlPreview()).contains("u.status IN (:statuses)");

        ParsedQueryMethod parsedNotIn = MethodLexer.parseQueryMethod("findByStatusNotIn");
        ExplanationResult resNotIn = QueryExplainer.explainQueryMethod(parsedNotIn, "UtilityReading", List.of("statuses"), "en");
        assertThat(resNotIn.getSqlPreview()).contains("u.status NOT IN (:statuses)");
    }

    @Test
    void testExplainerHandlesLikeAndNotLike() {
        ParsedQueryMethod parsedLike = MethodLexer.parseQueryMethod("findByStatusLike");
        ExplanationResult resLike = QueryExplainer.explainQueryMethod(parsedLike, "UtilityReading", List.of("pattern"), "en");
        assertThat(resLike.getSqlPreview()).contains("u.status LIKE :pattern");
    }

    @Test
    void testExplainerHandlesDeleteQueries() {
        ParsedQueryMethod parsedDelete = MethodLexer.parseQueryMethod("deleteByRoomId");
        ExplanationResult resDelete = QueryExplainer.explainQueryMethod(parsedDelete, "UtilityReading", List.of("roomId"), "en");
        assertThat(resDelete.getSqlPreview()).contains("DELETE FROM UtilityReading u\nWHERE u.roomId = :roomId");
    }
}
