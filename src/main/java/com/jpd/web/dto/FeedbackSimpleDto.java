package com.jpd.web.dto;

import java.time.LocalDate;
<<<<<<< HEAD
=======
import java.time.LocalDateTime;
>>>>>>> jpdWeb6/master

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSimpleDto {
    private Long feedbackId;
    private String content;
    private int rate;
<<<<<<< HEAD
    private LocalDate createDate;
=======
    private LocalDateTime createDate;
>>>>>>> jpdWeb6/master
    private CustomerSimpleDto customer;
}