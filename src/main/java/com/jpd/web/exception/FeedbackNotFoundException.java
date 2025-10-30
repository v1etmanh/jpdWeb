package com.jpd.web.exception;

public class FeedbackNotFoundException extends BusinessException{
    public FeedbackNotFoundException(Long id) {
        super("FEEDBACK_NOT_FOUND",
              "Feedback not found with id: " + id,
              "Phản hồi không được tìm thấy");
    }
}
