package com.jpd.web.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ModerationBatchResult;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.dto.ModuleContentUpdateResult;
import com.jpd.web.dto.RejectedContent;
import com.jpd.web.dto.RejectedContentInfo;
import com.jpd.web.dto.StandardizedContentDto;
import com.jpd.web.exception.ModerationException;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.Passage;
import com.jpd.web.model.ReadingQuestion;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.PassageRepository;
import com.jpd.web.repository.ReadingQuestionRepository;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.transform.ModuleContentBatchTransform;
import com.jpd.web.transform.ModuleContentTransform;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service

@Slf4j
public class ModuleContentService {

	@Autowired ModuleContentRepository moduleContentRepository;
	@Autowired
	private ValidationResources validationResources;
	@Autowired
	private ReadingQuestionRepository readingQuestionRepository;
	@Autowired
	private PassageRepository passageRepository;
	 @Autowired
	    private EntityManager entityManager;
	 @Autowired
	 private ContentModerationService moderationService;
	@Transactional
	public List<ModuleContent> updateCourseMaterial(ModuleContentDto moduleContentDto,long creatorId) {
		 Module module = validationResources.validateCompleteOwnership(
	                moduleContentDto.getModuleId(),
	                moduleContentDto.getChapterId(),
	                moduleContentDto.getCourseId(),
	                creatorId
	        );
	  
	    // 6️⃣ Kiểm tra moduleContent rỗng
	    List<ModuleContent> dtoContents = moduleContentDto.getModuleContent();
	 

	    // 7️⃣ Phân loại: MỚI vs ĐÃ TỒN TẠI
	    List<ModuleContent> toInsert = new ArrayList<>();
	    List<Long> idsToDelete = new ArrayList<>();
        
	    for (ModuleContent mc : dtoContents) {
	        mc.setModule(module);
	        
	        if (mc.getMcId() == null || mc.getMcId() < 0) {
	            // ✅ MỚI: Insert
	            mc.setMcId(null);
	            toInsert.add(mc);
	        } else {
	            // ✅ ĐÃ TỒN TẠI: Xóa rồi insert lại
	        	  
	        	   
	        	   
	            idsToDelete.add(mc.getMcId());
	            mc.setMcId(null);  // Set null để generate ID mới
	            toInsert.add(mc);
	        	   
	        }
	    }
	  

	    // 8️⃣ XÓA các bản ghi cũ trước
	    if (!idsToDelete.isEmpty()) {
	       
	        this.moduleContentRepository.deleteAllById(idsToDelete);
	        this.moduleContentRepository.flush();
	         // ⚠️ Quan trọng: Force delete ngay
	    }
	    this.entityManager.clear();

   //
	    
	    // 9️⃣ INSERT tất cả
	    
	   
	    List<ModuleContent>mds=  (List<ModuleContent>) moduleContentRepository.saveAll(toInsert);
	    return mds;
	}
	//delete 
	
	@Transactional
    public void deleteModuleContent(
            Long contentId, Long moduleId, Long chapterId, Long courseId, Long creatorId) {
        
        log.info("Deleting content {} from module {} by creator {}", 
                contentId, moduleId, creatorId);
        
        // Validate complete ownership
        Module module = validationResources.validateCompleteOwnership(
                moduleId, chapterId, courseId, creatorId
        );
        
        // Validate content exists and belongs to module
        ModuleContent content = moduleContentRepository.findById(contentId)
                .orElseThrow(() -> new ModuleContentNotFoundException(contentId));
        
        if (content.getModule().getModuleId()!=(moduleId)) {
            log.warn("Content {} does not belong to module {}", contentId, moduleId);
            throw new UnauthorizedException("This content does not belong to the specified module");
        }
        
        // Delete content
        moduleContentRepository.deleteById(contentId);
        
        log.info("Successfully deleted content {} from module {}", contentId, moduleId);
    }
	//delete by type @Transactional
	  @Transactional
	    public void deleteModuleContentsByType(
	            TypeOfContent type, Long moduleId, Long chapterId, Long courseId, Long creatorId) {
	        
	        log.info("Deleting all {} contents from module {} by creator {}", 
	                type, moduleId, creatorId);
	        
	        // Validate complete ownership
	        Module module = validationResources.validateCompleteOwnership(
	                moduleId, chapterId, courseId, creatorId );
	        
	        // Delete by type
	         moduleContentRepository.deleteByTypeOfContentAndModule(type, module);
	        
	        log.info("Successfully deleted {} {} contents from module {}", 
	                type.toString(), type, moduleId);
	    }
		@Transactional
	public List<ModuleContent>getModuleContentsByTypeAndModuleId( TypeOfContent type, Long moduleId, Long chapterId, Long courseId, Long creatorId){
		Module module = validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
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
		public ModuleContentUpdateResult updateCourseMaterial1(
		    ModuleContentDto moduleContentDto, long creatorId) {
		    
		    Module module = validationResources.validateCompleteOwnership(
		        moduleContentDto.getModuleId(),
		        moduleContentDto.getChapterId(),
		        moduleContentDto.getCourseId(),
		        creatorId
		    );
		    
		    Course c = module.getChapter().getCourse();
		    List<ModuleContent> dtoContents = moduleContentDto.getModuleContent();
		    
		    // ✅ TẠO MAP ĐỂ LƯU ID NHẤT QUÁN
		    Map<ModuleContent, Long> contentIdMap = new HashMap<>();
		    
		    // ✅ GÁN ID CHO TẤT CẢ TRƯỚC KHI TRANSFORM
		    for (ModuleContent mc : dtoContents) {
		        long contentId = mc.getMcId() != null && mc.getMcId() > 0 
		            ? mc.getMcId() 
		            : -Math.abs(System.nanoTime() + contentIdMap.size()); // Đảm bảo unique
		        contentIdMap.put(mc, contentId);
		    }
		    
		    // 1️⃣ Transform với ID từ Map
		    List<StandardizedContentDto> toModerate = new ArrayList<>();
		    for (ModuleContent mc : dtoContents) {
		        StandardizedContentDto dto = ModuleContentTransform.transform(
		            mc, 
		            c.getLanguage(), 
		            c.getTeachingLanguage()
		        );
		        // ✅ GHI ĐÈ CONTENT_ID TỪ MAP
		        dto.setContent_id(contentIdMap.get(mc));
		        toModerate.add(dto);
		    }
		    
		    if (toModerate.isEmpty()) {
		        throw new ModerationException("Failed to transform any content");
		    }
		    
		    // 2️⃣ Call moderation API
		    ModerationBatchResult moderationResult;
		    try {
		        moderationResult = moderationService.moderateBatchWithDetails(toModerate);
		    } catch (ModerationException e) {
		        log.error("Moderation service failed", e);
		        throw new ModerationException("Content moderation service unavailable: " + e.getMessage());
		    }
		    
		    // 3️⃣ Create map of approved content IDs
		    Set<Long> approvedIds = moderationResult.getApprovedContents().stream()
		        .map(StandardizedContentDto::getContent_id)
		        .collect(Collectors.toSet());
		    
		    // 4️⃣ Create rejection reason map
		    Map<Long, String> rejectionReasons = moderationResult.getRejectedContents().stream()
		        .collect(Collectors.toMap(
		            RejectedContentInfo::getContentId,
		            RejectedContentInfo::getReason
		        ));
		    
		    log.info("Moderation results: {} submitted, {} approved, {} rejected",
		        toModerate.size(), approvedIds.size(), rejectionReasons.size());
		    
		    // 5️⃣ Filter using consistent IDs from Map
		    List<ModuleContent> approvedModuleContents = dtoContents.stream()
		        .filter(mc -> approvedIds.contains(contentIdMap.get(mc))) // ✅ DÙNG ID TỪ MAP
		        .collect(Collectors.toList());
		    
		    // 6️⃣ Collect rejected contents
		    List<RejectedContent> rejectedContents = dtoContents.stream()
		        .filter(mc -> !approvedIds.contains(contentIdMap.get(mc))) // ✅ DÙNG ID TỪ MAP
		        .map(mc -> {
		            RejectedContent rejected = new RejectedContent();
		            rejected.setMcId(mc.getMcId());
		            rejected.setContent(ModuleContentTransform.extractRawContent(mc));
		            rejected.setType(mc.getTypeOfContent());
		            rejected.setReason(rejectionReasons.getOrDefault(
		                contentIdMap.get(mc), // ✅ DÙNG ID TỪ MAP
		                "Unknown reason"
		            ));
		            return rejected;
		        })
		        .collect(Collectors.toList());
		    
		    if (approvedModuleContents.isEmpty()) {
		        log.warn("All {} contents were rejected by moderation", dtoContents.size());
		        return new ModuleContentUpdateResult(Collections.emptyList(), rejectedContents);
		    }

		    // 7️⃣ - 9️⃣ Phần còn lại giữ nguyên
		    List<ModuleContent> toInsert = new ArrayList<>();
		    List<Long> idsToDelete = new ArrayList<>();
		    
		    for (ModuleContent mc : approvedModuleContents) {
		        mc.setModule(module);
		        
		        if (mc.getMcId() == null || mc.getMcId() < 0) {
		            mc.setMcId(null);
		            toInsert.add(mc);
		        } else {
		            idsToDelete.add(mc.getMcId());
		            mc.setMcId(null);
		            toInsert.add(mc);
		        }
		    }

		    if (!idsToDelete.isEmpty()) {
		        List<ModuleContent> toDeleteEntities = 
		            moduleContentRepository.findAllById(idsToDelete);
		        moduleContentRepository.deleteAllInBatch(toDeleteEntities);
		    }
		    
		    List<ModuleContent> savedContents = (List<ModuleContent>) 
		        moduleContentRepository.saveAll(toInsert);
		    
		    log.info("Update complete: {} saved, {} rejected", 
		        savedContents.size(), rejectedContents.size());
		    
		    return new ModuleContentUpdateResult(savedContents, rejectedContents);
		}
	
}
