package com.jpd.web.dto.Response;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
}
