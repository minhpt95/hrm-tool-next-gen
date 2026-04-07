package com.minhpt.hrmtoolnextgen.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends CommonException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String messageCode, Object... args) {
        super(messageCode, args, HttpStatus.NOT_FOUND);
    }
}
