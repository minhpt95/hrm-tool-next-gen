package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetCommandService timesheetCommandService;
    private final TimesheetQueryService timesheetQueryService;

    @Transactional
    public TimesheetDto createTimesheet(CreateTimesheetRequest form) {
        return timesheetCommandService.createTimesheet(form);
    }

    @Transactional
    public TimesheetDto updateTimesheet(UpdateTimesheetRequest form) {
        return timesheetCommandService.updateTimesheet(form);
    }

    @Transactional
    public TimesheetDto approvalTimesheet(ApprovalTimesheetRequest form) {
        return timesheetCommandService.approvalTimesheet(form);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TimesheetDto> getTimesheetsByManagerWithFilters(
            Long managerId,
            PaginationRequest paginationRequest,
            ETimesheetStatus status,
            Long projectId) {
        return timesheetQueryService.getTimesheetsByManagerWithFilters(managerId, paginationRequest, status, projectId);
    }
}
