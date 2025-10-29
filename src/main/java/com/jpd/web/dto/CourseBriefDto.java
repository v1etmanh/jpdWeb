package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class CourseBriefDto {
    private Long courseId;
    private String title;
    private String thumbnail;
    private String instructorName;
    private double price;
    private LocalDateTime enrolledAt;
    private double avgRating;
}
