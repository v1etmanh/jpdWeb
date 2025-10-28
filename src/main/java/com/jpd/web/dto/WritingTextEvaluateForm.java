package com.jpd.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.jpd.web.model.Language;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class WritingTextEvaluateForm {
	@Lob
	@NotBlank
	@JsonProperty("writingText")
private String writingText;
	@NotBlank
	@JsonProperty("language")
private String language;
}
