package com.jpd.web.exception;

public class ChapterNotFoundException extends BusinessException {
    public ChapterNotFoundException(Long id) {
        super("Chapter not found with id: " + id);
    }
}