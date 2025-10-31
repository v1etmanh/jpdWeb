package com.jpd.web.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {
    private Long kahootId;
  
    private String teacherName;
}