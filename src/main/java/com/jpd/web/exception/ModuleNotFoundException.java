package com.jpd.web.exception;

public class ModuleNotFoundException extends BusinessException {
	public ModuleNotFoundException(Long id) {
        super("MODULE_NOT_FOUND",
              "Module not found with id: " + id,
              "Mô-đun không được tìm thấy");
    }
}
