package com.jpd.web.controller.creator;

import java.util.List;

import com.google.api.Http;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.dto.ModuleContentUpdateResult;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.service.ModuleContentService;
import com.jpd.web.service.utils.RequestAttributeExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/api/creator/{courseId}/{chapterId}/{moduleId}")
@Slf4j
public class ModuleContentController {
	@Autowired
	private ModuleContentService moduleContentService;
	@GetMapping()
	public ResponseEntity<List<ModuleContent>> getModuleContentByTypeAndModuleId(@RequestParam("type") TypeOfContent typeOfContent,
																				 @Positive @PathVariable("moduleId") Long moduleId,
																				 @Positive @PathVariable("chapterId") Long chapterId,
																				 @Positive @PathVariable("courseId") Long courseId,
																				 HttpServletRequest request) {
		//return
	long creatorId=RequestAttributeExtractor.extractCreatorId(request);
		List<ModuleContent>mds=moduleContentService.getModuleContentsByTypeAndModuleId(typeOfContent,moduleId,chapterId,courseId,creatorId);
     
		return ResponseEntity.ok().body(mds);
	}
	@DeleteMapping("/{moduleContentId}")
	public ResponseEntity<?> deleteModuleContent(    @Positive @PathVariable("moduleContentId") long moduleContentId,
			@Positive @PathVariable("moduleId") Long moduleId,
            @Positive @PathVariable("chapterId") Long chapterId,
            @Positive @PathVariable("courseId") Long courseId,
            HttpServletRequest request               ) {

        Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
        moduleContentService.deleteModuleContent(
        		moduleContentId, moduleId, chapterId, courseId, creatorId
        );
        
        return ResponseEntity.noContent().build();
	  
	}
	@DeleteMapping("/deleteModuleContentByType")
	public ResponseEntity<?> deleteModuleContentByType(@RequestParam("type") TypeOfContent type,
			                          @Positive    @PathVariable ("moduleId")long moduleId,
			                          @Positive     @PathVariable ("chapterId")long chapterId,
			                          @Positive    @PathVariable ("courseId")long courseId,
			                              HttpServletRequest request   
	                                       ) {
	     Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
	        moduleContentService.deleteModuleContentsByType( type, moduleId, chapterId, courseId, creatorId);
	        return ResponseEntity.noContent().build();
	   
	}
	@PostMapping
    public ResponseEntity<?> updateModuleContents(
            @Valid @RequestBody ModuleContentDto moduleContentDto,
            HttpServletRequest request) {
		
        Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
        ModuleContentUpdateResult contents = moduleContentService.updateCourseMaterial1(moduleContentDto, creatorId);
        return ResponseEntity.ok(contents);
    }
}
