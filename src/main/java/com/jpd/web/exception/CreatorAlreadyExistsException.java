package com.jpd.web.exception;

public class CreatorAlreadyExistsException extends BusinessException {
	public CreatorAlreadyExistsException(String message) {
		 super("CREATOR_ALREADY_EXISTS", message,
	              "Tài khoản này đã tồn tại");
	}
}
