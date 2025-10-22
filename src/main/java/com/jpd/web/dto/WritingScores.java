package com.jpd.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class WritingScores {
   
	 private double grammar;
	    private double vocabulary;
	    private String feedback;

    // getters & setters
}