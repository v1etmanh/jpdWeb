package com.jpd.web.exception;

public class Creator_code_changePNotFoundException extends BusinessException {
	public Creator_code_changePNotFoundException(String message) {
		 super("CODE_NOT_FOUND", message,
	              "không tồn tại");
	}
}
