package com.jpd.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CreatorDtoResponse {
   private String name ;
    private String email ;
    private String description;
    private String avatar;
    private int totalStudents;
    private  int totalCourse;
    @Size(min=1,max=5)
    private int rating;
}
