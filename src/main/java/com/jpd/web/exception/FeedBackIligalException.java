package com.jpd.web.exception;

public class FeedBackIligalException extends BusinessException {

	public FeedBackIligalException(String message) {
        super("LANGUAGE_ILLIGAL", message,
              "ngôn từ bạn sử dụng quá khiếm nhã");
    }
    
    public FeedBackIligalException(String message, Throwable cause) {
        super("LANGUAGE_ILLIGAL", message, cause);
    }
	 
}