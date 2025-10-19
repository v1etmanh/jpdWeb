package com.jpd.web.exception;

public class ApiException extends BusinessException {
	  public ApiException(String message) {
	        super("API_ERROR", message,
	              "Có lỗi khi gọi API bên ngoài");
	    }
	    
	    public ApiException(String message, Throwable cause) {
	        super("API_ERROR", message, cause);
	    }
}
