package com.vatek.hrmtoolnextgen.component.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vatek.hrmtoolnextgen.dto.response.CommonErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Log4j2
@AllArgsConstructor
public class UnauthorizedHandler implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException e)
            throws IOException {

        log.error("Unauthorized error. Message : {} , {}", request.getRequestURL(), e);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        CommonErrorResponse body = CommonErrorResponse
                .commonErrorResponseBuilder()
                .message(e.getMessage())
                .httpStatusCode(HttpStatus.UNAUTHORIZED)
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
