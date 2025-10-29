package com.jpd.web.exception;

public class ModuleNotBelongsToChapterException extends BusinessException {
    public ModuleNotBelongsToChapterException(Long moduleId, Long chapterId) {
        super("MODULE_NOT_BELONGS_TO_CHAPTER",
              String.format("Module %d does not belong to chapter %d", moduleId, chapterId),
              "Mô-đun không thuộc chương này");
    }
}