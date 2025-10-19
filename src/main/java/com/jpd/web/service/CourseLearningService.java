package com.jpd.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CustomerModuleContentRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.CourseTransForm;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CourseLearningService {
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private ModuleContentRepository moduleContentRepository;
	@Transactional
	
	public CourseContentDto getCourseById(long courseId, String email )  {

		log.info("Retrieving course {} for creator {}", courseId, email);

		
		Course course = validationResources.validateCustomerWithCourse(email,courseId);

		course.getChapters().forEach(chapter -> {
			chapter.getModules();
			/*.forEach(module -> {

				List<ModuleContent> contents = this.moduleContentRepository.findByModule(module);

				module.setModuleContent(contents);
			});*/
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
		System.out.print(m.get());
		if(m.isPresent())
		 res.add(m.get());
		}
		return res;

		}
}
