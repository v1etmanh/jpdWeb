package com.jpd.web.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.exception.ChapterNotFoundException;
import com.jpd.web.exception.CourseNotFoundException;
import com.jpd.web.exception.CreatorNotFoundException;
import com.jpd.web.exception.ModuleNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Module;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ChapterService {

	
	@Autowired
	private ChapterRepository chapterRepository;
	@Autowired
	ModuleRepository moduleRepository;
	@Autowired
	ModuleContentRepository moduleContentRepository;

    @Autowired
    private ValidationResources validationResources;
	@Transactional
	public Chapter createChapter(String chapterName, Long courseId, Long creatorId) {
		 Course course=validationResources.validateCourseOwnership(courseId, creatorId);

		// Create chapter
		Chapter chapter = new Chapter();
		chapter.setChapterName(chapterName);
		chapter.setCourse(course);

		Chapter savedChapter = chapterRepository.save(chapter);
		log.info("Successfully created chapter {} for course {}", savedChapter.getChapterId(), courseId);

		return savedChapter;
	}

	@Transactional
	public void deleteChapter(long chapterId, long courseId, long creatorId)  {
		Course course=validationResources.validateCourseOwnership(courseId, creatorId);
		Chapter chapter = validationResources.validateChapterBelongsToCourse(chapterId, courseId);
		List<Module> modules = chapter.getModules();

		for (Module module : modules) {
			moduleContentRepository.deleteByModuleId(module.getModuleId());
		}

		moduleRepository.deleteByChapterChapterId(chapterId);

		chapterRepository.deleteByChapterId(chapterId);
	}
	public void updateChapter(long creatorId, String name,long chapterId) {
		 Creator c=  validationResources.validateCreatorExists(creatorId);
		   Optional<Chapter> m=this.chapterRepository.findById(chapterId);
		   if(m.isEmpty())throw new ModuleNotFoundException(chapterId);
		   if(m.get().getCourse().getCreator().getCreatorId()!=creatorId)
			   throw new UnauthorizedException("ko so huu module ");
		   m.get().setChapterName(name);
		   this.chapterRepository.save(m.get());
	}
	  
	
}
