package com.jpd.web.dto.Response;

import com.jpd.web.model.ModuleContent;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class ModuleDtoResponse {
     String titleOfModule;
     int orderInChapter;
    List<ModuleContent> moduleContent;


}
