package com.jpd.web.dto;

import com.jpd.web.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreatorListDto {
    private Long creatorId;
    private String fullName;
    private String email;
    private String imageUrl;
    private Status status;
    private Double balance;
    private Integer totalCourses;
    private Integer totalStudents;
    private Double avgRating;
    private Integer warningCount;
    private Date createDate;
}
