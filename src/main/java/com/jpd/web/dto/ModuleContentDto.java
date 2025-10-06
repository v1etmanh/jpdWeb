package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.ModuleContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class ModuleContentDto {
private long courseId;
private long chapterId;
private long moduleId;
private List<ModuleContent> moduleContent;
}
