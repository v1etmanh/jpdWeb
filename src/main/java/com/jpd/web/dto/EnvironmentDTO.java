package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentDTO {
    private List<PropertySource> propertySources;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySource {
        private String name;
        private Map<String, Object> properties;
    }
}