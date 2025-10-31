package com.jpd.web.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.jpd.web.controller.creator.CourseController;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Course;
import com.jpd.web.model.Customer;
import com.jpd.web.model.CustomerModuleContent;
import com.jpd.web.model.Enrollment;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CourseLearningService {

    private final CourseController courseController;
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private ModuleContentRepository moduleContentRepository;
	@Autowired
	private ModuleRepository moduleRepository;
	@Autowired
	private CustomerModuleContentRepository contentRepository;

    CourseLearningService(CourseController courseController) {
        this.courseController = courseController;
    }
	@Transactional
	
	public CourseContentDto getCourseById(long courseId, String email )  {

		log.info("Retrieving course {} for creator {}", courseId, email);

		
		Enrollment e = validationResources.validateCustomerWithCourseGetE(email,courseId);
		Course course=e.getCourse();
      if(course.isPublic()==false) throw new UnauthorizedException("this course is not exist");
		course.getChapters().forEach(chapter -> {
			chapter.getModules()
			.forEach(module -> {
				List<CustomerModuleContent> filteredContents = module.getCustomerModuleContents().stream()
	                    .filter(content -> content.getEnrollment().getEnrollId() == e.getEnrollId())
	                    .collect(Collectors.toList());

	            module.setCustomerModuleContents(filteredContents);
				
			});
		});
		CourseContentDto cdto = CourseTransForm.transformToCourseContentDto(course);
		return cdto;
	}
	@Transactional
	public List<ModuleContent>getModuleContentsByTypeAndModuleId( TypeOfContent type, Long moduleId, Long chapterId, Long courseId, String email){
		Module module = validationResources.validateModuleContentOwnerShip(moduleId, chapterId, courseId, email);
		List<ModuleContent> mds=this.moduleContentRepository.findByTypeOfContentAndModule(type, module);
		
		List<ModuleContent>res=new ArrayList<ModuleContent>();
		for(ModuleContent md:mds) {
			
		Optional<ModuleContent> m=	this.moduleContentRepository.findById(md.getMcId());
		
		if(m.isPresent())
		 res.add(m.get());
		}
		return res;

		}
	@Transactional
	public void updateCustomerFinishModule(long courseId, String email,long moduleId,TypeOfContent typeOfContent) {
		
		Enrollment e=this.validationResources.validateCustomerWithCourseGetE(email, courseId);
		Optional<Module>md=this.moduleRepository.findById(moduleId);
		if(md.isEmpty())throw new ModuleNotFoundException(moduleId);
		Module m=md.get();
		if(m.getChapter().getCourse().getCourseId()!=courseId)
			throw new UnauthorizedException("ban khong co quyen thuc hien tren khoa hc khac");
		if(!m.getContentTypes().contains(typeOfContent))
		throw new RuntimeException("module khong chua content do");
		Optional<CustomerModuleContent>cmds= contentRepository.findByEnrollmentAndModule(e,m);
		CustomerModuleContent x;
		 if(cmds.isEmpty()) {
		  x=CustomerModuleContent.builder()
				  .enrollment(e)
				  .module(m)
				  .availableRequest(5)
				  
				  .build();
		  x.setTypeOfContent(new HashSet<>());
		  }
		 else {x=cmds.get();}
		 
		 x.getTypeOfContent().add(typeOfContent);
		 this.contentRepository.save(x);
		//check module ton tai check xem co ton tai type of content nay trong module do khong
		// check module nay co thuoc ve khoa hc do khong 
		
	}
}
