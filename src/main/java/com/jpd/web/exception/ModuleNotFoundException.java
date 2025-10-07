package com.jpd.web.exception;

public class ModuleNotFoundException extends BusinessException {
	 public ModuleNotFoundException(Long id) {
	        super("Module not found with id: " + id);
	    }
}
