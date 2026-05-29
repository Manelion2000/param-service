package com.bakouan.app.service.parser;

public class ParserValidationException extends RuntimeException {
    public ParserValidationException(String message) {
        super(message);
    }

    public ParserValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
