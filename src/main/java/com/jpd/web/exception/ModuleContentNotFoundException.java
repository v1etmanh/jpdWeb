package com.jpd.web.exception;

public class ModuleContentNotFoundException extends BusinessException {
    public ModuleContentNotFoundException(Long contentId) {
        super("Module content not found with id: " + contentId);
    }
}