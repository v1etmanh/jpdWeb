package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.jpd.web.model.TypeOfContent;

@Data
@AllArgsConstructor // Bắt buộc cho JPQL constructor expression
public class ActivityCountDto {
    private long courseId;
    private long moduleId;
    private TypeOfContent typeOfContent;
    private long itemCount; // Số lượng mục con (ví dụ: 30 thẻ flashcard)
}