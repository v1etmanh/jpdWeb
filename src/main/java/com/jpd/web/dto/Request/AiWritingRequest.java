package com.jpd.web.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiWritingRequest {
    @NotBlank(message = "text is required")
    String text;
    @NotBlank(message = "language is required")
    String language;
    @NotBlank(message = "question is required")
    String question;
    String requirement;


}
