package com.jpd.web.dto;

import java.time.LocalDate;

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
    private LocalDate createDate;
    private CustomerSimpleDto customer;
}