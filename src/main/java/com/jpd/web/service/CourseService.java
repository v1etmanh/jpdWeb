package com.jpd.web.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.transform.CourseTransForm;

import jakarta.transaction.Transactional;

@Service
public class CourseService {
@Autowired
private FireBaseService fireBaseService;
@Autowired
private CustomerRepository customerRepository;
@Autowired
private CreatorRepository creatorRepository;
@Autowired
private CourseRepository courseRepository;
@Transactional
public Course createCourse(CourseFormDto courseFormDto,String email) throws Exception {
	Optional<Customer>c=this.customerRepository.findByEmail(email);
	if(c.isEmpty()) {
		throw new IllegalAccessException("this account not exist");
	}
	if(c.get().getCreator()==null) {
		throw new IllegalAccessException("creator is not exist");
	}
	Creator creator=c.get().getCreator();
	Course course=CourseTransForm.transformFromCourseFormDto(courseFormDto);
	if(course.getAccessMode()==AccessMode.PAID) {
		if(creator.getStatus()!=Status.SUCESS) {
			throw new IllegalAccessException("you must be update your payment and certifate if wanto to create course ");
		}
		 
        if(courseFormDto.getPrice() == 0 || courseFormDto.getPrice() <= 0) {
            throw new IllegalAccessException("Price must be greater than 0 for paid courses");
        }
	}
	String imgUrl=this.fireBaseService.uploadFile(courseFormDto.getImgFile(), TypeOfFile.IMG);
	if(imgUrl == null || imgUrl.isEmpty()) {
        throw new FileUploadException("Failed to upload course image");
    }
	
	course.setUrlImg(imgUrl);
	course.setCreator(creator);
	return this.courseRepository.save(course);
	  
}
public List<CourseCardDto> retrieveCourseByemail(String email) throws IllegalAccessException{
	Optional<Customer>c=this.customerRepository.findByEmail(email);
	if(c.isEmpty()) {
		throw new IllegalAccessException("this account not exist");
	}
	if(c.get().getCreator()==null) {
		throw new IllegalAccessException("creator is not exist");
	}
	List<Course>courses=c.get().getCreator().getCourses();
	return courses.stream().map(e->CourseTransForm.transformToCourseCardDto(e))
			.collect(Collectors.toList());
}
}
