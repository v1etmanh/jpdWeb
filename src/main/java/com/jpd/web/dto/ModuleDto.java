package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class ModuleDto {
private String title;
private long chapterId;
private long courseId;
}
