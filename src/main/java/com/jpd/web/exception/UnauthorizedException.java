package com.jpd.web.exception;

public class UnauthorizedException extends BusinessException {
	public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}