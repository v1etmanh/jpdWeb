package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.RememberWord;

public interface RememberWordRepository extends JpaRepository<RememberWord,Long>{

	List<RememberWord> findAllByCustomer_Email(String email);
	Page<RememberWord> findByWordContainingIgnoreCaseOrMeaningContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
	        String word, String meaning, String description, Pageable pageable);
}
