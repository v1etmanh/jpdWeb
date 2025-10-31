package com.jpd.web.controller;

import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.CourseLearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/customer/learning/{courseId}")
public class CourseLearningController {

    private final CreatorRepository creatorRepository;
    @Autowired
    private CourseLearningService courseLearningService;

    CourseLearningController(CreatorRepository creatorRepository) {
        this.creatorRepository = creatorRepository;
    }

    @GetMapping("/courseOverview")
    public ResponseEntity<CourseContentDto> getMethodName(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable("courseId") long courseId) {
        CourseContentDto contentDto = this.courseLearningService.getCourseById(courseId, jwt.getClaimAsString("email"));
        return ResponseEntity.ok(contentDto);
    }

    @GetMapping("/{chapterId}/{moduleId}/moduleContent")
    public ResponseEntity<?> retrieveModuleContent(@RequestParam("typeOfContent") TypeOfContent typeOfContent,
                                                   @AuthenticationPrincipal Jwt jwt, @PathVariable("courseId") long courseId,
                                                   @PathVariable("chapterId") long chapterId, @PathVariable("moduleId") long moduleId) {
        String email = jwt.getClaimAsString("email");
        List<ModuleContent> mds = this.courseLearningService.getModuleContentsByTypeAndModuleId(typeOfContent, moduleId, chapterId, courseId, email);
        return ResponseEntity.ok(mds);

    }

    @PostMapping("/{moduleId}/finish_content")
    public ResponseEntity<?> updateCustomerWithModuleContent(@PathVariable("courseId") long courseId, @PathVariable("moduleId") long moduleId, @RequestParam("type") TypeOfContent typeOfContent, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        this.courseLearningService.updateCustomerFinishModule(courseId, email, moduleId, typeOfContent);
        return ResponseEntity.noContent().build();
    }
}
