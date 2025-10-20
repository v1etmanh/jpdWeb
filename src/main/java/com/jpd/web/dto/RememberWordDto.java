package com.jpd.web.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class RememberWordDto {
    long rwId;
    String word;
    String meaning;
    String description;
}
