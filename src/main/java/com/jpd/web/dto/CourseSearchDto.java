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
public class CourseSearchDto {
    private long id;
    private String name;
    private String img;
    private long numberStudent;
    private double rating;
    private String instructor;
    private double price;
    private Language language;
}