package com.jpd.web.exception;

public class ChapterNotBelongsToCourseException extends BusinessException {
    public ChapterNotBelongsToCourseException(Long chapterId, Long courseId) {
        super("CHAPTER_NOT_BELONGS_TO_COURSE",
              String.format("Chapter %d does not belong to course %d", chapterId, courseId),
              "Chương không thuộc khóa học này");
    }
}