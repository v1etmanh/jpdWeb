package com.jpd.web.repository;

import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ModuleContentRepository extends JpaRepository<ModuleContent, Long> {
    List<ModuleContent> findByModule(Module module);

    @Modifying
    @Query("DELETE FROM ModuleContent mc WHERE mc.module.moduleId = :moduleId")
    void deleteByModuleId(@Param("moduleId") Long moduleId);

    void deleteByTypeOfContentAndModule(TypeOfContent typeOfContent, Module module);

    List<ModuleContent> findByTypeOfContentAndModule(TypeOfContent typeOfContent, Module module);

    List<ModuleContent> findByModule_Chapter_Course(Course moduleChapterCourse);



}
