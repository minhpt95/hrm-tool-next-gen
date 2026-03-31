package com.minhpt.hrmtoolnextgen.controller;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.dashboard.DashboardSummaryDto;
import com.minhpt.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.minhpt.hrmtoolnextgen.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Dashboard", description = "Dashboard metrics for administrators")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MessageService messageService;

    @GetMapping("/summary")
    @Operation(
            summary = "Dashboard summary",
            description = "Returns project counts grouped by status and number of active employees."
    )
    public ResponseEntity<CommonSuccessResponse<DashboardSummaryDto>> getDashboardSummary(HttpServletRequest request) {
        DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(buildSuccessResponse(summary, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .httpStatusCode(HttpStatus.OK)
                .message(messageService.getMessage("success"))
                .data(data)
                .build();
    }
}


