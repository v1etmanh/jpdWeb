package com.jpd.web.exception;

public class FileUploadException extends BusinessException {

	public FileUploadException(String message) {
        super("FILE_UPLOAD_ERROR", message,
              "Có lỗi khi tải file lên");
    }
    
    public FileUploadException(String message, Throwable cause) {
        super("FILE_UPLOAD_ERROR", message, cause);
    }
	 
}
