package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Chapter;

import jakarta.transaction.Transactional;

public interface ChapterRepository extends CrudRepository<Chapter, Long> {
	 @Modifying
	    @Transactional
	    @Query("DELETE FROM Chapter c WHERE c.chapterId = :chapterId")
	 int  deleteByChapterId(long chapterId);
}
