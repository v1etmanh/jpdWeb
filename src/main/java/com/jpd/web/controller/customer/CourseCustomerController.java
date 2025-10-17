package com.jpd.web.controller.customer;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.Response.CourseDetailResponse;
import com.jpd.web.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customer/course")
public class CourseCustomerController {
    @Autowired
    private  CourseService courseService;
    private static final Logger log = LoggerFactory.getLogger(CourseCustomerController.class);

    @GetMapping
    public List<CourseCardDto> getAllCourseCard(){
        return courseService.findALlCourse();
    }
    @GetMapping("/{courseId}")
    public CourseDetailResponse getCourseDetailById(@PathVariable("courseId") Long courseId){
        log.info("[GET] /api/customer/course/{} - received courseId={}", courseId, courseId);
        return courseService.getCourseDetailById(courseId);
    }
    @GetMapping("/search")
    public List<CourseCardDto> searchCourse(@RequestParam(required = false) String q) {
        log.info("[GET] /api/customer/course/search q={}", q);
        return courseService.searchCourse(q);
    }
}
