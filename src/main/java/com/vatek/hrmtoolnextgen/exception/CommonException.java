package com.vatek.hrmtoolnextgen.exception;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
@NoArgsConstructor
public class CommonException extends RuntimeException {
    private String message;
    private HttpStatusCode statusCode;
    private String messageCode;
    private Object[] messageArgs;

    public CommonException(String message) {
        this.message = message;
    }

    public CommonException(String message, HttpStatusCode statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public CommonException(String messageCode, Object[] messageArgs, HttpStatusCode statusCode) {
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
        this.statusCode = statusCode;
    }
}