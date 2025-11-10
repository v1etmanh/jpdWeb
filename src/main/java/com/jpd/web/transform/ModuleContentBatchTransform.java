package com.jpd.web.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.model.Language;
import com.jpd.web.model.ModuleContent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ModuleContentBatchTransform {
    
    /**
     * Transform multiple ModuleContents to StandardizedContentDtos
     * Handles errors gracefully - continues processing even if some items fail
     */
    public static List<StandardizedContentDto> transformBatch(
            List<ModuleContent> contents,Language l1,Language l2
    ) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<StandardizedContentDto> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (ModuleContent mc : contents) {
            try {
                StandardizedContentDto dto = ModuleContentTransform.transform(mc,l1,l2);
                if (dto != null && dto.getContent() != null && !dto.getContent().isEmpty()) {
                    results.add(dto);
                    successCount++;
                } else {
                    log.warn("Skipping empty content: mcId={}", mc.getMcId());
                    failCount++;
                }
            } catch (Exception e) {
                log.error("Failed to transform content mcId={}", mc.getMcId(), e);
                failCount++;
            }
        }
        
        log.info("Batch transform completed: {} success, {} failed", 
            successCount, failCount);
        
        return results;
    }
}