package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.Passage;

import jakarta.transaction.Transactional;

public interface PassageRepository extends JpaRepository<Passage, Long>{
	@Modifying
    @Transactional
    @Query("DELETE FROM Passage c WHERE c.mcId = :mcId")
 int  deleteByMcId(long mcId);
}
