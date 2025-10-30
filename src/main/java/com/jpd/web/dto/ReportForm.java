package com.jpd.web.dto;

import com.google.firebase.database.annotations.NotNull;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.ReportType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReportForm {
	@NotBlank
	private ReportType type;
	@NotBlank
	private String detail;

	@NotNull
	private long courseId;

	
}
