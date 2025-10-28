package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorSimpleDto {
    private Long creatorId;
    private String fullName;
    private String titleSelf;
    private String imageUrl;
    private String paymentEmail;
    private int totalCourses;
    private int totalStudents;
    private double averageRating;
}