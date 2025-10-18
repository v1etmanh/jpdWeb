package com.jpd.web.repository;

import com.jpd.web.dto.ActivityCountDto;
import com.jpd.web.dto.ContentTypeCountDTO;
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

    /**
     * Đếm số lượng của mỗi TypeOfContent cho một Module cụ thể.
     *
     * @param module Đối tượng Module cần thống kê.
     * @return Danh sách các đối tượng DTO chứa TypeOfContent và số lượng tương ứng.
     */
    @Query("SELECT new com.jpd.web.dto.ContentTypeCountDTO(mc.typeOfContent, COUNT(mc)) " +
            "FROM ModuleContent mc " +
            "WHERE mc.module = :module " +
            "GROUP BY mc.typeOfContent")
    List<ContentTypeCountDTO> countByTypeOfContentForModule(@Param("module") Module module);


    /**
     * Lấy danh sách tất cả các "Hoạt động học tập" và đếm số mục con trong mỗi hoạt động.
     * Áp dụng cho một danh sách các khóa học.
     */
    @Query("SELECT new com.jpd.web.dto.ActivityCountDto(c.courseId, m.moduleId, mc.typeOfContent, COUNT(mc.mcId)) " +
            "FROM ModuleContent mc " +
            "JOIN mc.module m " +
            "JOIN m.chapter ch " +
            "JOIN ch.course c " +
            "WHERE c IN :courses " +
            "GROUP BY c.courseId, m.moduleId, mc.typeOfContent")
    List<ActivityCountDto> countItemsInActivitiesByCourses(@Param("courses") List<Course> courses);


}
