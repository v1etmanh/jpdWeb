package com.jpd.web.exception;

public class PaypalEmailNotFoundException extends BusinessException {
	 public PaypalEmailNotFoundException(String message) {
	        super("PAYPAL_NOT_FOUND", message,
	              "bạn chưa đăng kí tài khoản paypal ");
	    }
}