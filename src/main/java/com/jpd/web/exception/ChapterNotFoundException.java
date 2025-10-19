package com.jpd.web.exception;

public class ChapterNotFoundException extends BusinessException {
	public ChapterNotFoundException(Long id) {
        super("CHAPTER_NOT_FOUND", 
              "Chapter not found with id: " + id,
              "Chương học không được tìm thấy");
    }
}