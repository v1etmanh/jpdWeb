package com.jpd.web.exception;

public class AIResponseParsingException extends RuntimeException {

    private String raw;

    public AIResponseParsingException(String message,String raw,Throwable cause){
        super(message,cause);
        this.raw=raw;
    }

    public String getRaw() {
        return raw;
    }
}
