package com.jpd.web.repository;

import com.jpd.web.model.WritingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WritingQuestionRepository extends JpaRepository<WritingQuestion, Long> {
}
