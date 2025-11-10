package com.jpd.web.exception;

public class ModerationException extends BusinessException {
	public ModerationException(String message) {
        super("CANNOT_MODERATE",message);
    }
   
}
