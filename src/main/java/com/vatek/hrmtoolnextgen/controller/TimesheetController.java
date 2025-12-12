package com.vatek.hrmtoolnextgen.controller;


import com.vatek.hrmtoolnextgen.constant.RoleConstant;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.service.TimesheetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
@AllArgsConstructor
@RequestMapping("/timesheet")
public class TimesheetController {
    private TimesheetService timesheetService;

    @PostMapping("/create")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> createTimesheet(
            @RequestBody CreateTimesheetRequest createTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.createTimesheet(createTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @PreAuthorize(RoleConstant.PROJECT_MANAGER)
    @PutMapping("/update")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> updateTimesheet(
            @RequestBody UpdateTimesheetRequest updateTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.updateTimesheet(updateTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @PreAuthorize(RoleConstant.PROJECT_MANAGER)
    @PutMapping("/decision")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> decisionTimesheet(
            ApprovalTimesheetRequest approvalForm,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.decisionTimesheet(approvalForm);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}
