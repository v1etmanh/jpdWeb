package com.jpd.web.repository;

import org.springframework.data.repository.CrudRepository;

import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import java.util.List;


public interface CourseRepository  extends CrudRepository<Course, Long>{
List<Course> findByAccessMode(AccessMode accessMode);
}
