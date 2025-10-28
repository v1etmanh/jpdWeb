package com.jpd.web.exception;

public class WithdrawException extends BusinessException{
	 public WithdrawException(String message) {
	        super("WITHDRAW_ERROR", message,
	              "Có lỗi khi xử lý yêu cầu rút tiền");
	    }
}
