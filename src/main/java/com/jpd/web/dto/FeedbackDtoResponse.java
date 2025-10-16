package com.jpd.web.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)

public class FeedbackDtoResponse {
    String user ;
    String context ;
    Date createAt;
    int rating;
    String avatar;

}
