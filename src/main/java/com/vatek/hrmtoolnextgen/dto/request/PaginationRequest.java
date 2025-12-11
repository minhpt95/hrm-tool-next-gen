package com.vatek.hrmtoolnextgen.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagination parameters")
public class PaginationRequest {
    @Schema(description = "Zero-based page index", example = "0")
    private Integer page;

    @Schema(description = "Page size", example = "10")
    private Integer size;

    @Schema(description = "Field to sort by", example = "id")
    private String sortBy;

    @Schema(description = "Sort direction ASC|DESC", example = "ASC")
    private String direction;

    public int getSafePage() {
        return page != null && page >= 0 ? page : 0;
    }

    public int getSafeSize() {
        int defaultSize = 10;
        if (size == null || size <= 0) return defaultSize;
        return size;
    }
}

