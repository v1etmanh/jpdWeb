package com.jpd.web.exception;

public class ModerateException extends BusinessException {
    public ModerateException() {
        super("IMG_INVALID",
                "Img is violation","ảnh có nội dung phản cảm ");
    }
}
