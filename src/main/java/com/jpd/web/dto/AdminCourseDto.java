    package com.jpd.web.dto;

    import com.jpd.web.model.Language;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class AdminCourseDto {
        private Long courseId;
        private String name;
        private String creatorName;
        private boolean isBan;
        private boolean isPublic;
        private double price;
        private Language language;
        private Language teachingLanguage;
    }
