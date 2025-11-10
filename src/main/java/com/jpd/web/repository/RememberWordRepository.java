package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.RememberWord;

public interface RememberWordRepository extends JpaRepository<RememberWord,Long>{

	List<RememberWord> findAllByCustomer_Email(String email);
	 
    @Query(value = "SELECT * FROM remember_word r WHERE " +
           "LOWER(r.word) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.meaning) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))",
           countQuery = "SELECT COUNT(*) FROM remember_word r WHERE " +
           "LOWER(r.word) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.meaning) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))",
           nativeQuery = true)
    Page<RememberWord> searchRememberWords(@Param("searchTerm") String searchTerm, Pageable pageable);
	
}
