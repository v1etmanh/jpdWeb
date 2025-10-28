package com.jpd.web.dto;

import org.springframework.web.multipart.MultipartFile;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Language;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class CourseFormDto {

    @NotBlank(message = "Course name is required")
    @Size(max = 255, message = "Course name must be less than 255 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description is too long")
    private String description;

    @NotBlank(message = "Target audience is required")
    @Size(max = 1000, message = "Target audience is too long")
    private String targetAudience;

    @Size(max = 2000, message = "Requirements are too long")
    private String requirements;

    @Size(max = 2000, message = "Learning objectives are too long")
    private String learningObject;

    @NotNull
    private Language language;
    @NotNull
    private Language teachingLanguage;
    @PositiveOrZero(message = "Price must be zero or positive")
    private double price;

   
    private MultipartFile imgFile;

    @NotNull(message = "Course type is required")
    private AccessMode accessMode;
}
