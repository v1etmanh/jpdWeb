package com.jpd.web.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RememberWordDto {
/* rwId: Date.now()
                word: word.trim(), 
                meaning: meaning.trim(), 
                description: description.trim() */
	private long rwId;
	private String word;
	private String meaning;
	private String description;
}
