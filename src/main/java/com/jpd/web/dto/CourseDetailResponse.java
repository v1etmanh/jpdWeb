package com.jpd.web.dto;

import lombok.*;

import java.sql.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder


public class CourseDetailResponse {
    private String name;
    private String description;
    private double rating;
    private int totalRatings;
    private int numberstudent;
    private String img;
    private double price;
    private double originalPrice;
    private double discount;
    private Date lastUpdated;
    private int totalLectures;
    private String language;
    private String learningOutcomes;
    private String requirements;
    private String targetAudience;
    private CreatorDtoResponse creator;
   private List<FeedbackDtoResponse> feedback;
   private List<?> topics;


}
