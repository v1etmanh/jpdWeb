package com.jpd.web.repository;

import com.jpd.web.model.ModuleContent;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface CourseRepository extends CrudRepository<Course, Long> {
    List<Course> findByAccessMode(AccessMode accessMode);

    @Query("select count(mc) from ModuleContent mc join mc.module m join m.chapter ch join ch.course c where c = " +
            ":course")
    int countModuleContentByCourse(@Param("course") Course course);


    @Query("SELECT DISTINCT c.language FROM Course c")
    List<String> findDistinctLanguages();

    List<Course> findByLanguage(String language);

}
