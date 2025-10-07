package com.jpd.web.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ModuleDto;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Module;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.transaction.Transactional;

@Service
public class ModuleService {
	@Autowired
	private ModuleRepository moduleRepository;
	@Autowired
	private CustomerRepository customerRepository;
	@Autowired
	private ModuleContentRepository moduleContentRepository;
	@Autowired
	private ValidationResources validationResources;

	public Module createModule(String moduleName, long creatorId, long courseId, long chapterId) {
		validationResources.validateCourseOwnership(courseId, creatorId);
        Chapter chapter = validationResources.validateChapterBelongsToCourse(chapterId, courseId);

		Module module = new Module();

		module.setChapter(chapter);
		module.setTitleOfModule(moduleName);

		return this.moduleRepository.save(module);
	}

	@Transactional
	public void deleteModule(long creatorId, long courseId, long chapterId, long moduleId)
		 {
		
	Module module=validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
		
		moduleContentRepository.deleteByModuleId(module.getModuleId());

		moduleRepository.deleteByModuleId(moduleId);
	}

}
