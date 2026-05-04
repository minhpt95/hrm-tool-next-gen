package com.minhpt.hrmtoolnextgen.service.dayoff;

import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalDayOffRequest;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDayOffRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DayOffService {

    private final DayOffCommandService dayOffCommandService;
    private final DayOffApprovalService dayOffApprovalService;

    @Transactional
    public DayOffDto createDayOffRequest(UserPrincipalDto userPrincipalDto, CreateDayOffRequest request) {
        return dayOffCommandService.createDayOffRequest(userPrincipalDto, request);
    }

    @Transactional
    public DayOffDto approveDayOffRequest(ApprovalDayOffRequest request, UserPrincipalDto userPrincipalDto) {
        return dayOffApprovalService.approveDayOffRequest(request, userPrincipalDto);
    }
}

