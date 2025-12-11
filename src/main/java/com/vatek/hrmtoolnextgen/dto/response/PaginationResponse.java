package com.vatek.hrmtoolnextgen.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Generic pagination wrapper")
public class PaginationResponse<T> {
    @Schema(description = "Current page index (zero-based)")
    private int currentPage;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Configured page size")
    private int pageSize;

    @Schema(description = "Total number of elements across all pages")
    private long totalElements;

    @Schema(description = "Number of elements in the current page")
    private int numberOfElements;

    @Schema(description = "Sorted by field")
    private String sortBy;

    @Schema(description = "Sort direction")
    private String direction;

    @Schema(description = "Is first page")
    private boolean first;

    @Schema(description = "Is last page")
    private boolean last;

    @Schema(description = "Page contents")
    private List<T> items;
}

