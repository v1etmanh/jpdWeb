package com.jpd.web.exception;

public class CommentNotFoundException extends BusinessException {
    public CommentNotFoundException(long commentId) {
    	super("COMMENT_NOT_FOUND", 
                "Comment not found with ID: " + commentId
                );
      }
    
}