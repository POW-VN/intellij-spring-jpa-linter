package com.pow.spring.jpa.linter;

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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class Suite100Test {

    private QueryValidator validator;
    private EntityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new EntityResolver();

        EntityInfo building = new EntityInfo("Building", "/dummy/Building.java");
        building.addField("id", "Long");
        building.addField("name", "String");
        building.addField("address", "String");

        EntityInfo room = new EntityInfo("Room", "/dummy/Room.java");
        room.addField("id", "Long");
        room.addField("roomNumber", "String");
        room.addField("building", "Building");
        room.addRelation("building", "Building");

        EntityInfo utilityReading = new EntityInfo("UtilityReading", "/dummy/UtilityReading.java");
        utilityReading.addField("id", "Long");
        utilityReading.addField("roomId", "Long");
        utilityReading.addField("room", "Room");
        utilityReading.addRelation("room", "Room");
        utilityReading.addField("billingMonth", "Integer");
        utilityReading.addField("billingYear", "Integer");
        utilityReading.addField("readingDate", "LocalDate");
        utilityReading.addField("electricityIndex", "Double");
        utilityReading.addField("waterIndex", "Double");
        utilityReading.addField("isLocked", "Boolean");
        utilityReading.addField("status", "String");

        resolver.registerEntity(building);
        resolver.registerEntity(room);
        resolver.registerEntity(utilityReading);

        validator = new QueryValidator(resolver);
    }

    private List<DiagnosticIssue> validate(String methodName, int paramCount, boolean hasQueryAnnotation) {
        JavaMethodInfo method = new JavaMethodInfo(methodName, "Object", hasQueryAnnotation, paramCount);
        return validator.validateMethod(method, "UtilityReading");
    }

    // -------------------------------------------------------------
    // NHÓM 1: VALID QUERIES (30 ca kiểm thử hợp lệ)
    // -------------------------------------------------------------
    static Stream<Arguments> validQueries() {
        return Stream.of(
                Arguments.of("findByRoomId", 1),
                Arguments.of("findByRoomIdAndBillingMonth", 2),
                Arguments.of("findByRoomIdAndBillingMonthAndBillingYear", 3),
                Arguments.of("findByRoomIdOrBillingYear", 2),
                Arguments.of("findByReadingDateBefore", 1),
                Arguments.of("findByReadingDateAfter", 1),
                Arguments.of("findByReadingDateBetween", 2),
                Arguments.of("findByBillingYearGreaterThan", 1),
                Arguments.of("findByBillingYearGreaterThanEqual", 1),
                Arguments.of("findByBillingYearLessThan", 1),
                Arguments.of("findByBillingYearLessThanEqual", 1),
                Arguments.of("findByStatusIsNull", 0),
                Arguments.of("findByStatusIsNotNull", 0),
                Arguments.of("findByStatusNotNull", 0),
                Arguments.of("findByIsLockedTrue", 0),
                Arguments.of("findByIsLockedFalse", 0),
                Arguments.of("findByStatusLike", 1),
                Arguments.of("findByStatusNotLike", 1),
                Arguments.of("findByStatusStartingWith", 1),
                Arguments.of("findByStatusEndingWith", 1),
                Arguments.of("findByStatusContaining", 1),
                Arguments.of("findByStatusIn", 1),
                Arguments.of("findByStatusNotIn", 1),
                Arguments.of("findByStatusIgnoreCase", 1),
                Arguments.of("findDistinctByRoomId", 1),
                Arguments.of("findTopByRoomId", 1),
                Arguments.of("findTop5ByRoomId", 1),
                Arguments.of("findFirstByRoomId", 1),
                Arguments.of("findFirst10ByRoomId", 1),
                Arguments.of("countByRoomId", 1),
                Arguments.of("existsByRoomIdAndBillingYear", 2),
                Arguments.of("deleteByRoomId", 1),
                Arguments.of("findTopByRoomIdOrderByBillingYearDesc", 1),
                Arguments.of("findTopByRoomIdOrderByBillingYearAsc", 1),
                Arguments.of("findTopByRoomIdOrderByBillingYearDescBillingMonthDesc", 1),
                Arguments.of("findTopByRoomIdOrderByBillingYearAscBillingMonthAsc", 1),
                Arguments.of("findByRoom_Id", 1),
                Arguments.of("findByRoom_Building_Name", 1),
                Arguments.of("findByRoom_Building_AddressContaining", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("validQueries")
    @DisplayName("Valid Queries should have 0 issues")
    void testValidQueries(String methodName, int paramCount) {
        List<DiagnosticIssue> issues = validate(methodName, paramCount, false);
        assertThat(issues).as("Expected 0 issues for %s", methodName).isEmpty();
    }

    // -------------------------------------------------------------
    // NHÓM 2: SAI TIỀN TỐ / MODIFIER / VERB (20 ca)
    // -------------------------------------------------------------
    static Stream<Arguments> prefixErrorQueries() {
        return Stream.of(
                Arguments.of("findsByRoomId", "Unknown query verb 'finds'"),
                Arguments.of("getsByRoomId", "Unknown query verb 'gets'"),
                Arguments.of("countsByRoomId", "Unknown query verb 'counts'"),
                Arguments.of("deletesByRoomId", "Unknown query verb 'deletes'"),
                Arguments.of("exitsByRoomId", "Unknown query verb 'exits'"),
                Arguments.of("seachByRoomId", "Unknown query verb 'seach'"),
                Arguments.of("queyByRoomId", "Unknown query verb 'quey'"),
                Arguments.of("radByRoomId", "Unknown query verb 'rad'"),
                Arguments.of("findTopssByRoomId", "Invalid limit modifier 'Topss'|Unrecognized modifier 'Topss'"),
                Arguments.of("findToppByRoomId", "Invalid limit modifier 'Topp'|Unrecognized modifier 'Topp'"),
                Arguments.of("findTop1sByRoomId", "Invalid limit modifier 'Top1s'|Unrecognized modifier 'Top1s'"),
                Arguments.of("findFirstsByRoomId", "Invalid limit modifier 'Firsts'|Unrecognized modifier 'Firsts'"),
                Arguments.of("findFirsttByRoomId", "Invalid limit modifier 'Firstt'|Unrecognized modifier 'Firstt'"),
                Arguments.of("findDisctinctByRoomId", "Distinct"),
                Arguments.of("findDistinctsByRoomId", "Distinct"),
                Arguments.of("findRoomId", "missing 'By'"),
                Arguments.of("getRoomId", "missing 'By'"),
                Arguments.of("countRoomId", "missing 'By'"),
                Arguments.of("deleteRoomId", "missing 'By'")
        );
    }

    @ParameterizedTest
    @MethodSource("prefixErrorQueries")
    @DisplayName("Prefix and modifier errors should be caught")
    void testPrefixErrorQueries(String methodName, String pattern) {
        List<DiagnosticIssue> issues = validate(methodName, 1, false);
        assertThat(issues).as("Expected error for %s", methodName).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(i.getMessage()).find());
        assertThat(matched).as("Pattern %s should match error message in %s", pattern, issues.get(0).getMessage()).isTrue();
    }

    // -------------------------------------------------------------
    // NHÓM 3: SAI TOÁN TỬ / OPERATOR TYPOS (15 ca)
    // -------------------------------------------------------------
    static Stream<Arguments> operatorErrorQueries() {
        return Stream.of(
                Arguments.of("findByBillingYearBetwen", "Between"),
                Arguments.of("findByBillingYearBetweeen", "Between"),
                Arguments.of("findByBillingYearGreateThan", "GreaterThan"),
                Arguments.of("findByBillingYearGreaterThen", "GreaterThan"),
                Arguments.of("findByBillingYearLessThen", "LessThan"),
                Arguments.of("findByBillingYearLessthan", "LessThan"),
                Arguments.of("findByStatusContaning", "Containing"),
                Arguments.of("findByStatusConatins", "Containing"),
                Arguments.of("findByStatusStartWith", "StartingWith"),
                Arguments.of("findByStatusStarttingWith", "StartingWith"),
                Arguments.of("findByStatusEndWith", "EndingWith"),
                Arguments.of("findByStatusIsNul", "IsNull"),
                Arguments.of("findByStatusIsNotNul", "IsNotNull"),
                Arguments.of("findByStatusNotin", "NotIn"),
                Arguments.of("findByStatusIgnorecase", "IgnoreCase")
        );
    }

    @ParameterizedTest
    @MethodSource("operatorErrorQueries")
    @DisplayName("Operator typos should be caught with suggestions")
    void testOperatorErrorQueries(String methodName, String expected) {
        List<DiagnosticIssue> issues = validate(methodName, 2, false);
        assertThat(issues).as("Expected error for %s", methodName).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().contains(expected));
        assertThat(matched).as("Expected suggestion '%s' in error message", expected).isTrue();
    }

    // -------------------------------------------------------------
    // NHÓM 4: SAI HƯỚNG & CÚ PHÁP SẮP XẾP ORDERBY (15 ca)
    // -------------------------------------------------------------
    static Stream<Arguments> orderByErrorQueries() {
        return Stream.of(
                Arguments.of("findTopByRoomIdOrderByBillingYearDescBillingMonthDescss", "Descss"),
                Arguments.of("findTopByRoomIdOrderByBillingYearDescBillingMonthDesssc", "Desssc"),
                Arguments.of("findTopByRoomIdOrderByBillingYearDescBillingMonthDescs", "Descs"),
                Arguments.of("findTopByRoomIdOrderByBillingYearDescBillingMonthDescc", "Descc"),
                Arguments.of("findTopByRoomIdOrderByBillingYearDes", "Des"),
                Arguments.of("findTopByRoomIdOrderByBillingYearDescending", "Descending"),
                Arguments.of("findTopByRoomIdOrderByBillingYearAscss", "Ascss"),
                Arguments.of("findTopByRoomIdOrderByBillingYearAsssc", "Asssc"),
                Arguments.of("findTopByRoomIdOrderByBillingYearAscc", "Ascc"),
                Arguments.of("findTopByRoomIdOrderByBillingYearAs", "As"),
                Arguments.of("findTopByRoomIdOrderByBillingYearAscending", "Ascending"),
                Arguments.of("findTopByRoomIdOrderbyBillingYearDesc", "Orderby"),
                Arguments.of("findTopByRoomIdOrderByyBillingYearDesc", "OrderByy"),
                Arguments.of("findTopByRoomIdOrderByBillingYearsDesc", "BillingYears"),
                Arguments.of("findTopByRoomIdOrderByRoom_Buildings_NameDesc", "Buildings")
        );
    }

    @ParameterizedTest
    @MethodSource("orderByErrorQueries")
    @DisplayName("OrderBy syntax and direction errors should be caught")
    void testOrderByErrorQueries(String methodName, String token) {
        List<DiagnosticIssue> issues = validate(methodName, 1, false);
        assertThat(issues).as("Expected error for %s", methodName).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().contains(token));
        assertThat(matched).as("Expected token '%s' in error message", token).isTrue();
    }

    // -------------------------------------------------------------
    // NHÓM 5: SAI TÊN THUỘC TÍNH ENTITY (15 ca)
    // -------------------------------------------------------------
    static Stream<Arguments> propertyErrorQueries() {
        return Stream.of(
                Arguments.of("findByRoomIds", "RoomIds"),
                Arguments.of("findByBillingYears", "BillingYears"),
                Arguments.of("findByBillingMonths", "BillingMonths"),
                Arguments.of("findByReadingDates", "ReadingDates"),
                Arguments.of("findByElectricityIndexs", "ElectricityIndexs"),
                Arguments.of("findByWaterIndexs", "WaterIndexs"),
                Arguments.of("findByIsLockeds", "IsLockeds"),
                Arguments.of("findByStatuss", "Statuss"),
                Arguments.of("findByRoom_Building_Names", "Names"),
                Arguments.of("findByRoom_Building_Addresss", "Addresss"),
                Arguments.of("findByRoom_Floor_Id", "Floor"),
                Arguments.of("findByInvalidField", "InvalidField"),
                Arguments.of("findByRoomIdAndUnknownField", "UnknownField"),
                Arguments.of("findByUnknownFieldOrBillingMonth", "UnknownField")
        );
    }

    @ParameterizedTest
    @MethodSource("propertyErrorQueries")
    @DisplayName("Property typo errors should be caught")
    void testPropertyErrorQueries(String methodName, String prop) {
        List<DiagnosticIssue> issues = validate(methodName, 2, false);
        assertThat(issues).as("Expected error for %s", methodName).isNotEmpty();
        boolean matched = issues.stream().anyMatch(i -> i.getMessage().contains(prop));
        assertThat(matched).as("Expected property '%s' in error message", prop).isTrue();
    }

    // -------------------------------------------------------------
    // NHÓM 6: THAM SỐ VÀ NGOẠI LỆ (10 ca)
    // -------------------------------------------------------------
    @Test
    void testParameterMismatchesAndExceptions() {
        // Between needs 2, provided 1 -> Warning
        assertThat(validate("findByBillingYearBetween", 1, false)).isNotEmpty();

        // Between needs 2, provided 0 -> Warning
        assertThat(validate("findByBillingYearBetween", 0, false)).isNotEmpty();

        // IsNull needs 0, provided 1 -> Warning
        assertThat(validate("findByStatusIsNull", 1, false)).isNotEmpty();

        // IsNotNull needs 0, provided 1 -> Warning
        assertThat(validate("findByStatusIsNotNull", 1, false)).isNotEmpty();

        // IsLockedTrue needs 0, provided 1 -> Warning
        assertThat(validate("findByIsLockedTrue", 1, false)).isNotEmpty();

        // RoomId needs 1, provided 0 -> Warning
        assertThat(validate("findByRoomId", 0, false)).isNotEmpty();

        // RoomId needs 1, provided 1 -> Valid (0 issues)
        assertThat(validate("findByRoomId", 1, false)).isEmpty();

        // Method with @Query annotation -> Always ignored (0 issues)
        assertThat(validate("customComplexQuery", 0, true)).isEmpty();

        // Builtin repository method 'save' -> Ignored (0 issues)
        assertThat(validate("save", 1, false)).isEmpty();
    }

    @Test
    @DisplayName("Kiểm tra phân giải quan hệ lồng nhau qua Entity liên kết (Contract -> Room.id, primaryTenant.id)")
    void testContractNestedRelationTraversal() {
        EntityInfo tenant = new EntityInfo("Tenant", "/dummy/Tenant.java");
        tenant.addField("id", "Long");
        tenant.addField("fullName", "String");

        EntityInfo contract = new EntityInfo("Contract", "/dummy/Contract.java");
        contract.addField("id", "Long");
        contract.addField("room", "Room");
        contract.addRelation("room", "Room");
        contract.addField("primaryTenant", "Tenant");
        contract.addRelation("primaryTenant", "Tenant");
        contract.addField("status", "String");

        resolver.registerEntity(tenant);
        resolver.registerEntity(contract);

        // findByRoomIdAndStatus with 2 params -> Valid
        JavaMethodInfo m1 = new JavaMethodInfo("findByRoomIdAndStatus", "Optional<Contract>", false, 2);
        assertThat(validator.validateMethod(m1, "Contract")).isEmpty();

        // existsByPrimaryTenantIdAndStatus with 2 params -> Valid
        JavaMethodInfo m2 = new JavaMethodInfo("existsByPrimaryTenantIdAndStatus", "boolean", false, 2);
        assertThat(validator.validateMethod(m2, "Contract")).isEmpty();

        // Typo: findByRoomIdssAndStatus -> Invalid (error on RoomIdss)
        JavaMethodInfo m3 = new JavaMethodInfo("findByRoomIdssAndStatus", "Optional<Contract>", false, 2);
        List<DiagnosticIssue> issues = validator.validateMethod(m3, "Contract");
        assertThat(issues).isNotEmpty();
        assertThat(issues.get(0).getMessage()).contains("RoomIdss");
    }
}
