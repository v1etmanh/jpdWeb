package com.jpd.web.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.exception.ModuleContentNotFoundException;
import com.jpd.web.exception.UnauthorizedException;
import com.jpd.web.model.KahootListFunction;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.PassageRepository;
import com.jpd.web.repository.ReadingQuestionRepository;
import com.jpd.web.service.utils.ValidationResources;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KahootModuleContentService {
@Autowired
private ModuleContentRepository moduleContentRepository;
@Autowired
private ValidationResources validationResources;
@Autowired
private ReadingQuestionRepository readingQuestionRepository;
@Autowired
private PassageRepository passageRepository;
 @Autowired
    private EntityManager entityManager;
@Transactional
public List<ModuleContent> updateCourseMaterial(List<ModuleContent> m,long kahootId,long creatorId) {
	 KahootListFunction k = validationResources.validateKahootOwnership(
                kahootId,
                creatorId
        );
  
    // 6️⃣ Kiểm tra moduleContent rỗng
	
    List<ModuleContent> dtoContents = m;
	

    // 7️⃣ Phân loại: MỚI vs ĐÃ TỒN TẠI
    List<ModuleContent> toInsert = new ArrayList<>();
    List<Long> idsToDelete = new ArrayList<>();
    
    for (ModuleContent mc : dtoContents) {
        mc.setKahootListFunction(k);
        
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
        Long contentId, Long kahootId, Long creatorId) {
    
    log.info("Deleting content {} from kahoot {} by creator {}", 
            contentId, kahootId, creatorId);
    
    // Validate complete ownership
    KahootListFunction k = validationResources.validateKahootOwnership(
            kahootId, creatorId
    );
    
    // Validate content exists and belongs to module
    ModuleContent content = moduleContentRepository.findById(contentId)
            .orElseThrow(() -> new ModuleContentNotFoundException(contentId));
    
    if (content.getKahootListFunction().getKahootId()!=(kahootId)) {
        log.warn("Content {} does not belong to module {}", contentId, kahootId);
        throw new UnauthorizedException("This content does not belong to the specified module");
    }
    
    // Delete content
    moduleContentRepository.deleteById(contentId);
    
    log.info("Successfully deleted content {} from module {}", contentId, kahootId);
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
	

}
