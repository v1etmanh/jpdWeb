package com.jpd.web.dto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularCourseDTO {
    private long courseId;
    private String title;
    private long students;
    private double revenue;
    private double rating;
    private String urlImg;
    private double price;
}