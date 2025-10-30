package com.jpd.web.exception;

public class ModuleContentNotFoundException extends BusinessException {
	 public ModuleContentNotFoundException(Long contentId) {
	        super("MODULE_CONTENT_NOT_FOUND",
	              "Module content not found with id: " + contentId,
	              "Nội dung mô-đun không được tìm thấy");
	    }
}