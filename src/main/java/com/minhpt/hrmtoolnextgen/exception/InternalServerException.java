package com.minhpt.hrmtoolnextgen.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends CommonException {

    public InternalServerException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public InternalServerException(String messageCode, Object... args) {
        super(messageCode, args, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
