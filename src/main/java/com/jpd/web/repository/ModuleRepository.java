package com.jpd.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Module;

import jakarta.transaction.Transactional;

public interface ModuleRepository extends CrudRepository<Module, Long> {
	 @Modifying
	    @Transactional
	    @Query("DELETE FROM Module m WHERE m.chapter.chapterId = :chapterId")
	    void deleteByChapterChapterId(@Param("chapterId") Long chapterId);
	 @Modifying
	 @Transactional
	 @Query("DELETE FROM Module m WHERE m.moduleId = :moduleId")
	 void deleteByModuleId(@Param("moduleId") Long moduleId);
	 

}
