package com.jpd.web.controller.creator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.ModuleDto;
import com.jpd.web.model.Module;
import com.jpd.web.service.ModuleService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PutMapping;


@RestController
@RequestMapping("/api/creator/{courseId}/{chapterId}/module")
public class ModuleController {
	@Autowired
	private ModuleService moduleService;

	@DeleteMapping("/{moduleId}")
	public ResponseEntity<?> deleteModule(
			@Positive(message = "Module ID must be positive") @PathVariable("moduleId") Long moduleId,
			@Positive(message = "Chapter ID must be positive") @PathVariable("chapterId") Long chapterId,
			@Positive(message = "Course ID must be positive") @PathVariable("courseId") Long courseId,
			HttpServletRequest request) {
		long creatorId = RequestAttributeExtractor.extractCreatorId(request);
		moduleService.deleteModule(creatorId, courseId, chapterId, moduleId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content

	}

	@PostMapping()
	public ResponseEntity<?> createModule(@Valid @RequestParam("moduleName") String moduleName,
			@Positive(message = "Course ID must be positive") @PathVariable("courseId") Long courseId,
			@Positive(message = "Chapter ID must be positive") @PathVariable("chapterId") Long chapterId,
			HttpServletRequest request) {
		// TODO: process POST request
         
		Long creatorId = RequestAttributeExtractor.extractCreatorId(request);

		// GlobalExceptionHandler sẽ xử lý exceptions
	Module module=	moduleService.createModule(moduleName, creatorId, courseId, chapterId);

		return ResponseEntity.status(HttpStatus.CREATED).body(module);

	}
	@PutMapping("/{moduleId}/update")
	public ResponseEntity<?> putMethodName(@PathVariable("moduleId") long moduleId, @RequestParam String name,
			HttpServletRequest request) {
		//TODO: process PUT request
		Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
		this.moduleService.updateModuleName(creatorId, moduleId, name);
		return ResponseEntity.noContent().build();
	}
}
