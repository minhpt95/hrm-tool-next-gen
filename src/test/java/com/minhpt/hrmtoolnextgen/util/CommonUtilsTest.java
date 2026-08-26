package com.minhpt.hrmtoolnextgen.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;

/**
 * Unit tests for {@link CommonUtils} — pageable construction and its sort defaults,
 * pagination-response assembly, random password generation, and money-to-words.
 */
class CommonUtilsTest {

    // -------------------------------------------------------------------------
    // buildPageable — defaults to sorting by id ASC
    // -------------------------------------------------------------------------

    @Test
    void buildPageable_twoArg_sortsByIdAscending() {
        Pageable pageable = CommonUtils.buildPageable(2, 25);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertEquals(Sort.by(Direction.ASC, "id"), pageable.getSort());
    }

    @Test
    void buildPageable_withDirectionAndProperties() {
        Pageable pageable = CommonUtils.buildPageable(0, 10, Direction.DESC, "name", "id");

        assertEquals(Sort.by(Direction.DESC, "name", "id"), pageable.getSort());
    }

    @Test
    void buildPageable_blankSortBy_fallsBackToId() {
        assertEquals(Sort.by(Direction.ASC, "id"),
                CommonUtils.buildPageable(0, 10, "   ", "ASC").getSort());
        assertEquals(Sort.by(Direction.ASC, "id"),
                CommonUtils.buildPageable(0, 10, null, "ASC").getSort());
    }

    @Test
    void buildPageable_unrecognisedDirection_fallsBackToAscending() {
        assertEquals(Sort.by(Direction.ASC, "name"),
                CommonUtils.buildPageable(0, 10, "name", "sideways").getSort());
        assertEquals(Sort.by(Direction.ASC, "name"),
                CommonUtils.buildPageable(0, 10, "name", null).getSort());
    }

    @Test
    void buildPageable_directionIsCaseInsensitive() {
        assertEquals(Sort.by(Direction.DESC, "name"),
                CommonUtils.buildPageable(0, 10, "name", "desc").getSort());
    }

    // -------------------------------------------------------------------------
    // buildPageable(PaginationRequest) — routes through the safe accessors
    // -------------------------------------------------------------------------

    @Test
    void buildPageable_fromRequest_usesSuppliedValues() {
        PaginationRequest request = PaginationRequest.builder()
                .page(3).size(50).sortBy("email").direction("DESC").build();

        Pageable pageable = CommonUtils.buildPageable(request);

        assertEquals(3, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertEquals(Sort.by(Direction.DESC, "email"), pageable.getSort());
    }

    @Test
    void buildPageable_fromRequest_appliesSafeDefaults() {
        // Negative page and non-positive size fall back to 0 and 10 respectively.
        Pageable pageable = CommonUtils.buildPageable(
                PaginationRequest.builder().page(-5).size(0).build());

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.by(Direction.ASC, "id"), pageable.getSort());
    }

    @Test
    void buildPageable_fromEmptyRequest_usesAllDefaults() {
        Pageable pageable = CommonUtils.buildPageable(PaginationRequest.builder().build());

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }

    // -------------------------------------------------------------------------
    // buildPageableWithDefaultSort
    // -------------------------------------------------------------------------

    @Test
    void buildPageableWithDefaultSort_appliesCallerDefaultsWhenUnset() {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(
                PaginationRequest.builder().page(0).size(10).build(), "createdDate", "DESC");

        assertEquals(Sort.by(Direction.DESC, "createdDate"), pageable.getSort());
    }

    @Test
    void buildPageableWithDefaultSort_nullCallerDefaults_useCreatedDateDesc() {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(
                PaginationRequest.builder().page(0).size(10).build(), null, null);

        assertEquals(Sort.by(Direction.DESC, "createdDate"), pageable.getSort());
    }

    @Test
    void buildPageableWithDefaultSort_explicitRequestValuesWin() {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(
                PaginationRequest.builder().page(0).size(10).sortBy("name").direction("ASC").build(),
                "createdDate", "DESC");

        assertEquals(Sort.by(Direction.ASC, "name"), pageable.getSort());
    }

    @Test
    void buildPageableWithDefaultSort_blankRequestValuesFallBack() {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(
                PaginationRequest.builder().page(0).size(10).sortBy("  ").direction("  ").build(),
                "updatedDate", "ASC");

        assertEquals(Sort.by(Direction.ASC, "updatedDate"), pageable.getSort());
    }

    // -------------------------------------------------------------------------
    // buildPaginationResponse
    // -------------------------------------------------------------------------

    @Test
    void buildPaginationResponse_copiesPageMetadataAndNormalisesSort() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);
        PaginationRequest request = PaginationRequest.builder()
                .page(0).size(2).sortBy("name").direction("DESC").build();

        PaginationResponse<String> response = CommonUtils.buildPaginationResponse(page, request);

        assertEquals(0, response.getCurrentPage());
        assertEquals(3, response.getTotalPages());
        assertEquals(2, response.getPageSize());
        assertEquals(5, response.getTotalElements());
        assertEquals(2, response.getNumberOfElements());
        assertEquals("name", response.getSortBy());
        assertEquals("DESC", response.getDirection());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertEquals(List.of("a", "b"), response.getItems());
    }

    @Test
    void buildPaginationResponse_blankSort_normalisesToIdAsc() {
        Page<String> page = new PageImpl<>(List.of("only"), PageRequest.of(0, 10), 1);

        PaginationResponse<String> response = CommonUtils.buildPaginationResponse(
                page, PaginationRequest.builder().build());

        assertEquals("id", response.getSortBy());
        assertEquals("ASC", response.getDirection());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void buildPaginationResponse_emptyPage_reportsZeroes() {
        Page<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        PaginationResponse<String> response = CommonUtils.buildPaginationResponse(
                page, PaginationRequest.builder().build());

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getNumberOfElements());
        assertTrue(response.getItems().isEmpty());
    }

    // -------------------------------------------------------------------------
    // buildPaginationRequestForResponse
    // -------------------------------------------------------------------------

    @Test
    void buildPaginationRequestForResponse_keepsPagingAndSubstitutesSort() {
        PaginationRequest original = PaginationRequest.builder()
                .page(2).size(20).sortBy("ignored").direction("ignored").build();

        PaginationRequest result = CommonUtils.buildPaginationRequestForResponse(original, "id", "ASC");

        assertEquals(2, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals("id", result.getSortBy());
        assertEquals("ASC", result.getDirection());
    }

    // -------------------------------------------------------------------------
    // randomPassword
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {1, 8, 12, 32})
    void randomPassword_hasRequestedLengthAndIsAlphanumeric(int length) {
        String password = CommonUtils.randomPassword(length);

        assertEquals(length, password.length());
        assertTrue(password.chars().allMatch(Character::isLetterOrDigit),
                () -> "not alphanumeric: " + password);
    }

    @Test
    void randomPassword_zeroLength_returnsEmptyString() {
        assertEquals("", CommonUtils.randomPassword(0));
    }

    @Test
    void randomPassword_successiveCallsDiffer() {
        // A 40-char alphanumeric collision is vanishingly unlikely; this guards
        // against a constant/seeded implementation.
        assertNotEquals(CommonUtils.randomPassword(40), CommonUtils.randomPassword(40));
    }

    // -------------------------------------------------------------------------
    // convertMoneyToText
    // -------------------------------------------------------------------------

    @Test
    void convertMoneyToText_spellsOutTheAmountWithCurrency() {
        String text = CommonUtils.convertMoneyToText("123", Locale.US);

        assertNotNull(text);
        assertEquals(text.toUpperCase(), text, "result is upper-cased");
        assertTrue(text.contains("USD"), () -> "expected currency code in: " + text);
        assertTrue(text.contains("HUNDRED"), () -> "expected spelled-out amount in: " + text);
    }

    @Test
    void convertMoneyToText_nonNumericInput_fallsBackToZero() {
        String text = CommonUtils.convertMoneyToText("not-a-number", Locale.US);

        assertTrue(text.startsWith("0"), () -> "expected zero fallback in: " + text);
        assertTrue(text.contains("USD"));
    }

    @Test
    void convertMoneyToText_nullLocale_usesSystemDefault() {
        // Must not throw; the default locale supplies the currency.
        assertNotNull(CommonUtils.convertMoneyToText("10", null));
    }
}
