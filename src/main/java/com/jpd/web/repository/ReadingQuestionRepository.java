package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.ReadingQuestion;

import jakarta.transaction.Transactional;

import com.jpd.web.model.Passage;


public interface ReadingQuestionRepository extends JpaRepository<ReadingQuestion,Long> {
 int deleteByPassage_McId(long mcId);
 void deleteByPassage(Passage passage);
 @Modifying
 @Transactional
 @Query(value = "DELETE FROM reading_question WHERE mc_id = :mcId", nativeQuery = true)
 void deleteByPassageIdNative(@Param("mcId") long mcId);
 @Modifying
 @Transactional
 @Query(
     value = "DELETE FROM reading_question_options WHERE rq_id IN (SELECT rq_id FROM reading_question WHERE mc_id = :mcId)",
     nativeQuery = true
 )
 void deleteQuestionOptionsByPassageId(@Param("mcId") long mcId);
}
