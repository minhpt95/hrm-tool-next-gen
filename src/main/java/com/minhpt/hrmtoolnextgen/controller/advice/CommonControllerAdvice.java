package com.minhpt.hrmtoolnextgen.controller.advice;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.response.CommonErrorResponse;
import com.minhpt.hrmtoolnextgen.dto.response.CommonResponse;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.CommonException;
import com.minhpt.hrmtoolnextgen.exception.InternalServerException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class CommonControllerAdvice {

    private final MessageService messageService;

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
        return ResponseEntity.status(errorResponse.getHttpStatusCode()).body(errorResponse);
    }
}
