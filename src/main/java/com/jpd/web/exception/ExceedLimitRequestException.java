package com.jpd.web.exception;

public class ExceedLimitRequestException extends BusinessException {

	public ExceedLimitRequestException(String message) {
		  super("EXCEED_LIMIT", message,
	              "Bạn đã vượt quá giới hạn yêu cầu");
		// TODO Auto-generated constructor stub
	}

}
