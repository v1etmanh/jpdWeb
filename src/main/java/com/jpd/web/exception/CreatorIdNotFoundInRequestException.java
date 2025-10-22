package com.jpd.web.exception;

public class CreatorIdNotFoundInRequestException extends BusinessException {
    public CreatorIdNotFoundInRequestException() {
        super("CREATOR_ID_NOT_FOUND",
              "Creator ID not found in request attributes",
              "Không tìm thấy ID tác giả trong yêu cầu");
    }
    
    public CreatorIdNotFoundInRequestException(String details) {
        super("CREATOR_ID_NOT_FOUND",
              "Creator ID not found in request attributes: " + details,
              "Không tìm thấy ID tác giả trong yêu cầu");
    }
}