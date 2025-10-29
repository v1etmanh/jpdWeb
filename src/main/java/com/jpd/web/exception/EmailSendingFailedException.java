package com.jpd.web.exception;

public class EmailSendingFailedException extends BusinessException {
    public EmailSendingFailedException(String recipientEmail) {
        super("EMAIL_SENDING_FAILED",
              "Failed to send email to: " + recipientEmail,
              "Không thể gửi email, vui lòng thử lại sau");
    }
    
    public EmailSendingFailedException(String recipientEmail, Throwable cause) {
        super("EMAIL_SENDING_FAILED",
              "Failed to send email to: " + recipientEmail,
              cause);
    }
}