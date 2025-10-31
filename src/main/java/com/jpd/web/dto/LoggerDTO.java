package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoggerDTO {
    private String name;
    private String configuredLevel;
    private String effectiveLevel;
}

