package com.jpd.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RememberWordDto {

	
	private long rwId;
	@NotBlank
	private String word;
	@NotBlank
	private String meaning;
	@NotBlank
	private String description;
}
