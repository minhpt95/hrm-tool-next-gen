package com.minhpt.hrmtoolnextgen.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadRequestException extends CommonException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String messageCode, Object... args) {
        super(messageCode, args, HttpStatus.BAD_REQUEST);
    }
}
