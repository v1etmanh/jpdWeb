package com.jpd.web.exception;

public class KahootNotFoundException extends BusinessException {
	public KahootNotFoundException(Long khId) {
        super("MODULE_CONTENT_NOT_FOUND",
              "Module content not found with id: " + khId,
              "Nội dung mô-đun không được tìm thấy");
    }
}
