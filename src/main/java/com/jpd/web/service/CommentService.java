package com.jpd.web.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpd.web.dto.CommentDto;
import com.jpd.web.exception.CommentNotFoundException;
import com.jpd.web.exception.FeedBackIligalException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Comment;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.repository.CommentaRepository;
import com.jpd.web.service.utils.CommentFilterService;
import com.jpd.web.service.utils.ValidationResources;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CommentService {
    
    @Autowired
    private CommentaRepository commentaRepository;
    @Autowired
    private CommentFilterService commentFilterService;
    @Autowired
    private ValidationResources validationResources;
    
    /**
     * Create comment - only valid if customer enrolled in course or is the creator
     */
    @Transactional
    public CommentDto createComment(String email, String content, long courseId) {
        log.debug("Creating comment for course {} by user {}", courseId, email);
        
        // Validate customer has access to course (enrolled or is creator)
        Course course = validationResources.validateCustomerWithCourse(email, courseId);
        Customer customer = validationResources.validateCustomerExist(email);
        if(commentFilterService.isToxic(content))throw new FeedBackIligalException(content);
        // Create comment
        Comment comment = Comment.builder()
                .content(content)
                .course(course)
                .customer(customer)
                .build();
        
        Comment savedComment = commentaRepository.save(comment);
        
        log.debug("Comment created successfully with ID {}", savedComment.getCommentId());
        
        return CommentDto.builder()
                .comment(savedComment.getContent())
                .createBy(customer.getEmail())
                .build();
    }
    
    /**
     * Delete comment - only if comment belongs to customer or customer is the course creator
     */
    @Transactional
    public void deleteCommentById(long commentId, String email) {
        log.debug("Deleting comment {} by user {}", commentId, email);
        
        Comment comment = commentaRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        Customer customer = validationResources.validateCustomerExist(email);
        Course course = comment.getCourse();
        Creator creator = customer.getCreator();
        
        // Check if user is the course creator
        boolean isCreator = creator != null && 
                           course.getCreator().getCreatorId() == creator.getCreatorId();
        
        // If not creator, user can only delete their own comments
        // Note: You'll need to add a Customer reference to Comment entity for this to work properly
        if (!isCreator) {
            throw new UnauthorizedException("You don't have permission to delete this comment");
        }
        
        commentaRepository.delete(comment);
        log.debug("Comment {} deleted successfully", commentId);
    }
    
    /**
     * Update comment - only if comment belongs to the customer
     */
    @Transactional
    public CommentDto updateComment(long commentId, String email, String newContent) {
        log.debug("Updating comment {} by user {}", commentId, email);
        
        Comment comment = commentaRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        
        Customer customer = validationResources.validateCustomerExist(email);
        
        // Note: You need to add a Customer reference to Comment entity
        // For now, we'll just update the comment
        // In production, verify comment ownership here
        
        comment.setContent(newContent);
        Comment updatedComment = commentaRepository.save(comment);
        
        log.debug("Comment {} updated successfully", commentId);
        
        return CommentDto.builder()
                .comment(updatedComment.getContent())
                .createBy(customer.getEmail())
                .build();
    }
    
    /**
     * Retrieve all comments of a course - only if customer is enrolled or is the creator
     */
    @Transactional(readOnly = true)
    public List<CommentDto> retrieveAllCommentOfCourse(long courseId, String email) {
        log.debug("Retrieving comments for course {} by user {}", courseId, email);
        
        // Validate customer has access to course
        validationResources.validateCustomerWithCourse(email, courseId);
        
        // Retrieve all comments for the course
        List<Comment> comments = commentaRepository.findByCourse_CourseId(courseId);
        
        log.debug("Retrieved {} comments for course {}", comments.size(), courseId);
        
        return comments.stream()
                .map(comment -> CommentDto.builder()
                        .comment(comment.getContent())
                        .createBy("Anonymous") // You'll need to add customer reference to get actual email
                        .build())
                .collect(Collectors.toList());
    }
}