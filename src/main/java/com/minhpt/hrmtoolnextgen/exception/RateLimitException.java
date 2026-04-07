package com.minhpt.hrmtoolnextgen.exception;

import org.springframework.http.HttpStatus;

public class RateLimitException extends CommonException {

    public RateLimitException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
