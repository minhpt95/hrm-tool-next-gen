package com.minhpt.hrmtoolnextgen.controller.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.response.CommonErrorResponse;
import com.minhpt.hrmtoolnextgen.dto.response.CommonResponse;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.CommonException;
import com.minhpt.hrmtoolnextgen.exception.InternalServerException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.exception.RateLimitException;
import com.minhpt.hrmtoolnextgen.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@RestControllerAdvice
@RequiredArgsConstructor
@Log4j2
public class CommonControllerAdvice {

    private final MessageService messageService;

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<CommonResponse> handleNotFoundException(
            HttpServletRequest request,
            NotFoundException ex) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<CommonResponse> handleRateLimitException(
            HttpServletRequest request,
            RateLimitException ex) {
        return buildErrorResponse(ex, request);
    }

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

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<CommonResponse> handleUnauthorizedException(
            HttpServletRequest request,
            UnauthorizedException ex) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<CommonResponse> handleValidationException(
            HttpServletRequest request,
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());

        CommonResponse errorResponse = CommonErrorResponse
                .commonErrorResponseBuilder()
                .message(message)
                .httpStatusCode(HttpStatus.BAD_REQUEST)
                .path(request.getServletPath())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<CommonResponse> handleUnexpectedException(
            HttpServletRequest request,
            Exception ex) {
        log.error("Unexpected error on {}: {}", request.getServletPath(), ex.getMessage(), ex);
        CommonResponse errorResponse = CommonErrorResponse
                .commonErrorResponseBuilder()
                .message("An unexpected error occurred")
                .httpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .path(request.getServletPath())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ResponseEntity<CommonResponse> buildErrorResponse(
            CommonException ex,
            HttpServletRequest request) {

        String resolvedMessage;
        if (ex.getMessageCode() != null) {
            resolvedMessage = messageService.getMessage(ex.getMessageCode(),
                    ex.getMessageArgs() != null ? ex.getMessageArgs() : new Object[]{});
        } else {
            resolvedMessage = ex.getMessage();
        }

        CommonResponse errorResponse = CommonErrorResponse
                .commonErrorResponseBuilder()
                .message(resolvedMessage)
                .httpStatusCode(ex.getStatusCode())
                .path(request.getServletPath())
                .build();
        return ResponseEntity.status(Objects.requireNonNull(errorResponse.getHttpStatusCode())).body(errorResponse);
    }
}
