package com.vatek.hrmtoolnextgen.util;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.util.Currency;
import java.util.Locale;


public class CommonUtils {


    public static Pageable buildPageable(int pageIndex, int pageSize) {
        // Default sort by id to ensure deterministic ordering across endpoints
        return buildPageable(pageIndex, pageSize, (String) null, null);
    }

    public static Pageable buildPageable(int pageIndex, int pageSize,Direction direction,String... properties) {
        return PageRequest.of(pageIndex, pageSize,direction,properties);
    }

    public static Pageable buildPageable(int pageIndex, int pageSize, String sortBy, String direction) {
        String sortProperty = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Direction dir = Direction.fromOptionalString(direction).orElse(Direction.ASC);
        return PageRequest.of(pageIndex, pageSize, Sort.by(dir, sortProperty));
    }

    public static Pageable buildPageable(PaginationRequest paginationRequest) {
        return buildPageable(
                paginationRequest.getSafePage(),
                paginationRequest.getSafeSize(),
                paginationRequest.getSortBy(),
                paginationRequest.getDirection()
        );
    }

    public static <T> PaginationResponse<T> buildPaginationResponse(Page<T> page, PaginationRequest paginationRequest) {
        String sortBy = (paginationRequest.getSortBy() == null || paginationRequest.getSortBy().isBlank())
                ? "id"
                : paginationRequest.getSortBy();
        String direction = Direction.fromOptionalString(paginationRequest.getDirection())
                .orElse(Direction.ASC)
                .name();

        return PaginationResponse.<T>builder()
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .numberOfElements(page.getNumberOfElements())
                .sortBy(sortBy)
                .direction(direction)
                .first(page.isFirst())
                .last(page.isLast())
                .items(page.getContent())
                .build();
    }

    /**
     * Builds pageable with default sort by createdDate desc if not specified
     */
    public static Pageable buildPageableWithDefaultSort(PaginationRequest paginationRequest, String defaultSortBy, String defaultDirection) {
        String sortBy = paginationRequest.getSortBy();
        String direction = paginationRequest.getDirection();
        
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = defaultSortBy != null ? defaultSortBy : "createdDate";
        }
        if (direction == null || direction.isBlank()) {
            direction = defaultDirection != null ? defaultDirection : "DESC";
        }
        
        return buildPageable(
            paginationRequest.getSafePage(),
            paginationRequest.getSafeSize(),
            sortBy,
            direction
        );
    }

    /**
     * Builds pagination request for response with normalized sort values
     */
    public static PaginationRequest buildPaginationRequestForResponse(
            PaginationRequest originalRequest, 
            String actualSortBy, 
            String actualDirection) {
        return PaginationRequest.builder()
            .page(originalRequest.getPage())
            .size(originalRequest.getSize())
            .sortBy(actualSortBy)
            .direction(actualDirection)
            .build();
    }

    public static String randomPassword(int length){
        return RandomStringUtils.secure().nextAlphanumeric(length);
    }

    public static String convertMoneyToText(String input,Locale locale) {
        String output;

        if(locale == null){
            locale = Locale.getDefault();
        }

        Currency currency = Currency.getInstance(locale);

        try {
            NumberFormat ruleBasedNumberFormat = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);
            output = ruleBasedNumberFormat.format(Long.parseLong(input)) + " " + currency;
        } catch (Exception e) {
            output = 0 + " " + currency;
        }
        return output.toUpperCase();
    }
}
