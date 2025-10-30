package com.jpd.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;


public interface ModuleContentRepository extends JpaRepository<ModuleContent, Long> {
List<ModuleContent> findByModule(Module module);
@Modifying
@Query("DELETE FROM ModuleContent mc WHERE mc.module.moduleId = :moduleId")
void deleteByModuleId(@Param("moduleId") Long moduleId);
void deleteByTypeOfContentAndModule(TypeOfContent typeOfContent, Module module);
List<ModuleContent> findByTypeOfContentAndModule(TypeOfContent typeOfContent, Module module);
ModuleContent  deleteByMcId(Long mcId);
    @Modifying
    @Query("DELETE FROM ModuleContent mc WHERE mc.kahootListFunction.kahootId = :kahootId")
    void deleteByKahootId(@Param("kahootId") Long kahootId);
}
