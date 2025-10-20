package com.jpd.web.exception;

public class AIHandlerException extends BusinessException {

    public AIHandlerException(String message) {
        super("ERROR_WITH_AI_HANDLER", message,
              "Có lỗi xảy ra khi xử lý AI, vui lòng thử lại sau");
    }
    
    public AIHandlerException(String message, Throwable cause) {
        super("ERROR_WITH_AI_HANDLER", message, cause);
    }

}
