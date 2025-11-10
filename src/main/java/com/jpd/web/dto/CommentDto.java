package com.jpd.web.dto;

import java.sql.Date;
import java.util.List;

import com.jpd.web.model.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
private String comment;
private String createBy;
}
