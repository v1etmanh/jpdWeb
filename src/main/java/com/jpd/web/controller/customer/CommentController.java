package com.jpd.web.controller.customer;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.CommentDto;
import com.jpd.web.dto.CommentRequestDto;
import com.jpd.web.service.CommentService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/courses/{courseId}/comments")
@Slf4j
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    /**
     * Get all comments of a course
     * GET /api/courses/{courseId}/comments
     */
    @GetMapping
    public ResponseEntity<List<CommentDto>> getAllComments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("courseId") long courseId) {
        
        log.debug("Getting all comments for course {}", courseId);
        String email = jwt.getClaimAsString("email");
        
        List<CommentDto> comments = commentService.retrieveAllCommentOfCourse(courseId, email);
        return ResponseEntity.ok(comments);
    }
    
    /**
     * Create a new comment
     * POST /api/courses/{courseId}/comments
     */
    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("courseId") long courseId,
            @Valid @RequestBody CommentRequestDto request) {
        
        log.debug("Creating comment for course {}", courseId);
        String email = jwt.getClaimAsString("email");
        
        CommentDto comment = commentService.createComment(email, request.getContent(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }
    
    /**
     * Update a comment
     * PUT /api/courses/{courseId}/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("courseId") long courseId,
            @PathVariable("commentId") long commentId,
            @Valid @RequestBody CommentRequestDto request) {
        
        log.debug("Updating comment {} for course {}", commentId, courseId);
        String email = jwt.getClaimAsString("email");
        
        CommentDto comment = commentService.updateComment(commentId, email, request.getContent());
        return ResponseEntity.ok(comment);
    }
    
    /**
     * Delete a comment
     * DELETE /api/courses/{courseId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("courseId") long courseId,
            @PathVariable("commentId") long commentId) {
        
        log.debug("Deleting comment {} from course {}", commentId, courseId);
        String email = jwt.getClaimAsString("email");
        
        commentService.deleteCommentById(commentId, email);
        return ResponseEntity.noContent().build();
    }
}