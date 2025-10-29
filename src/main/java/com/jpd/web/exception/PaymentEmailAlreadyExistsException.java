package com.jpd.web.exception;

public class PaymentEmailAlreadyExistsException extends BusinessException {
	public PaymentEmailAlreadyExistsException(String message) {
        super("PAYMENT_EMAIL_ALREADY_EXISTS", message,
              "Email thanh toán này đã tồn tại");
    }
}
