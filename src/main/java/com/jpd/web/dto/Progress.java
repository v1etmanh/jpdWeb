package com.jpd.web.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progress {
    long total;
    long done = 0;
}
