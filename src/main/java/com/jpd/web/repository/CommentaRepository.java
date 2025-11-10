package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jpd.web.model.Comment;

@Repository
public interface CommentaRepository extends JpaRepository<Comment,Long>{
	List<Comment> findByCourse_CourseId(long courseId);
}
