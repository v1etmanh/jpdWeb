package com.jpd.web.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
	private String code;
	private String message;
	private String userMessage; // Thân thiện với user
	private String path;
	private LocalDateTime timestamp;
	private Object details;
	private String traceId; // Để tracking logs

	// ✅ Added for frontend compatibility
	private boolean success = false;
	private int status;
	private String responseType;

	public static ErrorResponse of(String code, String message) {
		return ErrorResponse.builder()
				.code(code)
				.message(message)
				.timestamp(LocalDateTime.now())
				.build();
	}

	public static ErrorResponse of(String code, String message,
			String path) {
		return ErrorResponse.builder()
				.code(code)
				.message(message)
				.path(path)
				.timestamp(LocalDateTime.now())
				.build();
	}
}