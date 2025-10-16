package com.jpd.web.dto;

import com.jpd.web.model.ModuleContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModuleContentGroupDto {
    private ModuleContent moduleContent;
    private long count;
}