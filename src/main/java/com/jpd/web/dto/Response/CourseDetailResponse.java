package com.jpd.web.dto.Response;

import com.jpd.web.model.Chapter;
import com.jpd.web.model.Creator;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Date;
import java.time.LocalDate;
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
    private LocalDate lastUpdated;
    private int totalLectures;
    private String language;
    private String learningOutcomes;
    private String requirements;
    private String targetAudience;
    private CreatorDtoResponse creator;
   private List<FeedbackDtoResponse> feedback;
   private List<Chapter> curriculum;


}
