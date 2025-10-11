package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Module;
import com.jpd.web.model.TypeOfContent;

import jakarta.transaction.Transactional;

public interface ModuleRepository extends JpaRepository<Module, Long> {
	@Modifying
	@Transactional
	@Query("DELETE FROM Module m WHERE m.chapter.chapterId = :chapterId")
	void deleteByChapterChapterId(@Param("chapterId") Long chapterId);

	@Modifying
	@Transactional
	@Query("DELETE FROM Module m WHERE m.moduleId = :moduleId")
	void deleteByModuleId(@Param("moduleId") Long moduleId);

	@Query("SELECT DISTINCT mc.typeOfContent FROM Module m " + "JOIN m.moduleContent mc WHERE m.moduleId = :moduleId")
	List<TypeOfContent> findTypeOfContentByModuleId(@Param("moduleId") Long moduleId);

}
