package com.vatek.hrmtoolnextgen.controller.advice;

import com.vatek.hrmtoolnextgen.dto.response.CommonErrorResponse;
import com.vatek.hrmtoolnextgen.dto.response.CommonResponse;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.exception.CommonException;
import com.vatek.hrmtoolnextgen.exception.InternalServerException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class CommonControllerAdvice {
    
    @ExceptionHandler(InternalServerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<CommonResponse> handleInternalServerException(
            HttpServletRequest request, 
            InternalServerException ex) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse> handleBadRequestException(
            HttpServletRequest request, 
            BadRequestException ex) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<CommonResponse> handleInsufficientAuthentication(
            HttpServletRequest request,
            InsufficientAuthenticationException ex) {
        CommonResponse errorResponse = CommonErrorResponse
                .commonErrorResponseBuilder()
                .message(ex.getMessage())
                .httpStatusCode(HttpStatus.UNAUTHORIZED)
                .path(request.getServletPath())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    private ResponseEntity<CommonResponse> buildErrorResponse(
            CommonException ex, 
            HttpServletRequest request) {
        CommonResponse errorResponse = CommonErrorResponse
                .commonErrorResponseBuilder()
                .message(ex.getMessage())
                .httpStatusCode(ex.getStatusCode())
                .path(request.getServletPath())
                .build();
        return ResponseEntity.status(errorResponse.getHttpStatusCode()).body(errorResponse);
    }
}
