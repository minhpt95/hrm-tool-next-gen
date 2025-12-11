package com.vatek.hrmtoolnextgen.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class UnauthorizedException extends CommonException {
    public UnauthorizedException(String message, HttpStatusCode statusCode) {
        super(message, statusCode);
    }

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

