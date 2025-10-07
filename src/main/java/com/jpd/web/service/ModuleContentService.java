package com.jpd.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service

@Slf4j
public class ModuleContentService {
	@Autowired
	private CustomerRepository customerRepository;
	@Autowired ModuleContentRepository moduleContentRepository;
	@Autowired
	private ValidationResources validationResources;
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
	        System.out.println("Deleting IDs: " + idsToDelete);
	        moduleContentRepository.deleteAllById(idsToDelete);
	         // ⚠️ Quan trọng: Force delete ngay
	    }

	    // 9️⃣ INSERT tất cả
	    System.out.println("Inserting " + toInsert.size() + " records");
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
	                moduleId, chapterId, courseId, creatorId
	        );
	        
	        // Delete by type
	         moduleContentRepository.deleteByTypeOfContentAndModule(type, module);
	        
	        log.info("Successfully deleted {} {} contents from module {}", 
	                type.toString(), type, moduleId);
	    }
}
