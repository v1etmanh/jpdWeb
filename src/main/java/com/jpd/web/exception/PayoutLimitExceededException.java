package com.jpd.web.exception;

public class PayoutLimitExceededException extends BusinessException {
	 public PayoutLimitExceededException(String message) {
	        super("PAYOUT_LIMIT_EXCEEDED", message,
	              "Bạn đã vượt quá giới hạn rút tiền");
	    }
}
