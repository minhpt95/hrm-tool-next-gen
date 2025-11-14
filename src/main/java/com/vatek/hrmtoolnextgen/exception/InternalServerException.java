package com.vatek.hrmtoolnextgen.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InternalServerException extends CommonException {
    
    public InternalServerException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
