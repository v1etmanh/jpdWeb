package com.jpd.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.jpd.web.model.Language;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
	@NotEmpty
	private List<String> synonyms;
	@NotEmpty
	private List<String> example;
	private Language language;
	private Integer voteCount; // Số lượng vote
	private String customerEmail; // Email của người tạo từ
}
