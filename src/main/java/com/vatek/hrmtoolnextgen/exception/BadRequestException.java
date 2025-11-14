package com.vatek.hrmtoolnextgen.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadRequestException extends CommonException {
    
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
