package com.jpd.web.repository;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;


public interface CourseRepository extends CrudRepository<Course, Long> {
    List<Course> findByAccessMode(AccessMode accessMode);


    @Query("SELECT DISTINCT c.language FROM Course c")
    List<String> findDistinctLanguages();

    List<Course> findByLanguage(String language);


}
