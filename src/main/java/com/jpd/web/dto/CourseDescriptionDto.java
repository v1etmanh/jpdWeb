package com.jpd.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Language;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDescriptionDto {
    // Course basic info
    private Long courseId;
    private String name;
    private String description;
    private Language language;
    private Language teachingLanguage;
    private double price;
    private String urlImg;
    private LocalDate createdAt;
    private LocalDate lastUpdate;
    private boolean isPublic;
    private boolean isBan;
    private String accessMode;
    
    // Parsed fields
    private String learningObject;
    private String requirements;
    private String targetAudience;
    
    // Creator info
    private CreatorSimpleDto creator;
    
    // Chapters/Modules
    private List<Chapter> chapters;
    
    // Statistics
    private int totalStudents;
    private int totalFeedbacks;
    private double averageRating;
    private int totalModules;
    
    // Feedback list
    private List<FeedbackSimpleDto> feedbacks;
}