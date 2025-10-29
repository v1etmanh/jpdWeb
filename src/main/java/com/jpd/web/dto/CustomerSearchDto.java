package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSearchDto {
    private Long customerId;
    private String email;
    private String username;
    private LocalDateTime createdAt;
}
