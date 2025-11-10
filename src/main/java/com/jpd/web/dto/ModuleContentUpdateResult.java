package com.jpd.web.dto;

import java.util.List;

import com.jpd.web.model.ModuleContent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ModuleContentUpdateResult {
	  private List<ModuleContent> approved;
	    private List<RejectedContent> rejected;
	    
}
