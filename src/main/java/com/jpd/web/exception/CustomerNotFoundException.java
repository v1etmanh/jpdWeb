package com.jpd.web.exception;

public class CustomerNotFoundException  extends BusinessException{
	 public CustomerNotFoundException(String message) {
	        super("CUSTOMER_NOT_FOUND", message,
	              "Khách hàng không được tìm thấy");
	    }
}
