package com.jpd.web.exception;

public class BusinessException extends RuntimeException {
	 private String errorCode;
	    private String userMessage; // Message hiển thị cho user
	    private Object details; // Thêm context data
	    
	    public BusinessException(String errorCode, String message) {
	        super(message);
	        this.errorCode = errorCode;
	        this.userMessage = message;
	    }
	    
	    public BusinessException(String errorCode, String message, 
	                            String userMessage) {
	        super(message);
	        this.errorCode = errorCode;
	        this.userMessage = userMessage;
	    }
	    
	    public BusinessException(String errorCode, String message, 
	                            Throwable cause) {
	        super(message, cause);
	        this.errorCode = errorCode;
	        this.userMessage = message;
	    }
	    
	    public BusinessException(String errorCode, String message, 
	                            Object details, Throwable cause) {
	        super(message, cause);
	        this.errorCode = errorCode;
	        this.userMessage = message;
	        this.details = details;
	    }
	    
	    public String getErrorCode() {
	        return errorCode;
	    }
	    
	    public String getUserMessage() {
	        return userMessage;
	    }
	    
	    public Object getDetails() {
	        return details;
}
}