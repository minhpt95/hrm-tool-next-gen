package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.service.TimesheetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping("/admin")
@Tag(name = "Admin", description = "CRUD APIs for Admin User")
public class UserController {

    private final TimesheetService timesheetService;

    @PostMapping("/create")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> createTimesheet(
            @RequestBody CreateTimesheetRequest createTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.createTimesheet(createTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }

    @PutMapping("/update")
    public ResponseEntity<CommonSuccessResponse<TimesheetDto>> updateTimesheet(
            @RequestBody UpdateTimesheetRequest updateTimesheetReq,
            HttpServletRequest request
    ) {
        TimesheetDto timesheetDto = timesheetService.updateTimesheet(updateTimesheetReq);

        return ResponseEntity.ok(buildSuccessResponse(timesheetDto, request));
    }


    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .data(data)
                .build();
    }
}

