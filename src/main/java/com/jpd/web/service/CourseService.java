package com.jpd.web.service;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseContentDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.dto.ModuleDto;
import com.jpd.web.model.AccessMode;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Creator;
import com.jpd.web.model.Customer;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.PendingImage;
import com.jpd.web.model.Status;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.repository.ChapterRepository;
import com.jpd.web.repository.CourseRepository;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.repository.ModuleContentRepository;
import com.jpd.web.repository.ModuleRepository;
import com.jpd.web.repository.PendingImgRepository;
import com.jpd.web.transform.CourseTransForm;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
@Slf4j
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
@Autowired
private ModuleContentRepository  moduleContentRepository;
@Autowired
private GeminiAiService geminiAiService;
@Autowired
private PendingImgRepository pendingImgRepository;
@Autowired
private ModuleRepository moduleRepository;
@Autowired
private ChapterRepository chapterRepository;
@Autowired
private static SecureRandom secureRandom;
public static String generate6DigitCode() {
    int number = 100000 + secureRandom.nextInt(900000);
    return String.valueOf(number);
}
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
	if(course.getAccessMode()==AccessMode.PRIVATE) {
		course.setJoinKey(generate6DigitCode());
	}
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
@Transactional
public CourseContentDto getCourseById(long id, String email) throws IllegalAccessException {
	Optional<Customer>c=this.customerRepository.findByEmail(email);
	if(c.isEmpty()) {
		throw new IllegalAccessException("this account not exist");
	}
	Creator creator=c.get().getCreator();
	if(creator==null) {
		throw new IllegalAccessException("creator is not exist");
	}
	Optional<Course>couse=this.courseRepository.findById(id);
	if(couse.isEmpty()) {
		throw new IllegalAccessException("course is not exist");
	}
	if(!creator.getCourses().contains(couse.get())) {
		throw new IllegalAccessException("course is not belong to you");
	}
Course courseEntity = couse.get();
    
    // ========== THÊM ĐOẠN NÀY ==========
    // Load đúng data cho TẤT CẢ modules
    courseEntity.getChapters().forEach(chapter -> {
        chapter.getModules().forEach(module -> {
            // Query lại để load đúng data
            List<ModuleContent> contents = this.moduleContentRepository.findByModule(module);
            // Gán lại vào module
            module.setModuleContent(contents);
        });
    });
	CourseContentDto cdto=CourseTransForm.transformToCourseContentDto(courseEntity);
	return cdto;
}
public String generateFeedback(String question, String answer,String email) throws IllegalAccessException {
	Optional<Customer>c=this.customerRepository.findByEmail(email);
	if(c.isEmpty()) {
		throw new IllegalAccessException("this account not exist");
	}
	Creator creator=c.get().getCreator();
	if(creator==null) {
		throw new IllegalAccessException("creator is not exist");
	}
	
	StringBuilder sb = new StringBuilder();
	sb.append("You are a multilingual language tutor.\n");
	sb.append("Your task is to analyze the learner's answer in ANY language.\n");
	sb.append("Feedback must be written in clear and natural Vietnamese.\n");
	sb.append("Feedback length: 1–3 sentences, no lists, no JSON, no extra formatting.\n\n");

	sb.append("The feedback must include:\n");
	sb.append("1. Confirmation: nói câu trả lời đúng hay sai.\n");
	sb.append("2. Explanation: giải thích ngắn gọn về ngữ pháp hoặc từ vựng.\n");
	sb.append("Câu hỏi: \"").append(escapeForPrompt(question)).append("\"\n");
	sb.append("Câu trả lời: \"").append(escapeForPrompt(answer)).append("\"\n");
    return this.geminiAiService.generateContent(sb.toString());
}

/**
 * Minimal escaping for quotes and newlines so the prompt stays single-line friendly.
 * Adjust if you have more complex input (e.g., triple quotes or JSON inside question/answer).
 */
private String escapeForPrompt(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ");
}

public String saveImgIntoFirebase(MultipartFile img,String email,TypeOfFile type) throws IllegalAccessException {
	Optional<Customer>c=this.customerRepository.findByEmail(email);
	if(c.isEmpty()) {
		throw new IllegalAccessException("this account not exist");
	}
	Creator creator=c.get().getCreator();
	if(creator==null) {
		throw new IllegalAccessException("creator is not exist");
	}
	try {
		
		String url= this.fireBaseService.uploadFile(img, type);
		PendingImage p=new PendingImage();
		p.setCreatorId(creator.getCreatorId());
		p.setStatus(Status.PENDING);
		p.setUrl(url);
		this.pendingImgRepository.save(p);
		return url;
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	}
}
@Transactional
public List<ModuleContent> updateCourseMaterial(ModuleContentDto moduleContentDto, String email) throws IllegalAccessException {
    // 1️⃣ Kiểm tra customer tồn tại
    Customer customer = customerRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    // 2️⃣ Kiểm tra creator tồn tại
    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }

    // 3️⃣ Tìm course
    Course course = creator.getCourses().stream()
        .filter(c -> c.getCourseId() == moduleContentDto.getCourseId())
        .findFirst()
        .orElseThrow(() -> new IllegalAccessException("Course not found"));

    // 4️⃣ Tìm chapter
    Chapter chapter = course.getChapters().stream()
        .filter(ch -> ch.getChapterId() == moduleContentDto.getChapterId())
        .findFirst()
        .orElseThrow(() -> new IllegalAccessException("Chapter not found"));

    // 5️⃣ Kiểm tra module tồn tại trong chapter
    Module module = chapter.getModules().stream()
        .filter(m -> m.getModuleId() == moduleContentDto.getModuleId())
        .findFirst()
        .orElseThrow(() -> new IllegalAccessException("Module not found in chapter"));

    // 6️⃣ Kiểm tra moduleContent rỗng
    List<ModuleContent> dtoContents = moduleContentDto.getModuleContent();
    if (dtoContents.isEmpty()) {
        throw new IllegalAccessException("Module content list is empty");
    }

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
//
public void deleteModuleContent(String email, long id) throws IllegalAccessException {
	Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    // 2. Kiểm tra creator
    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }
    Optional<ModuleContent> m=this.moduleContentRepository.findById(id);
    if(m.isEmpty()) {
    	 throw new IllegalAccessException("id does not exist");
    }
 Creator cg=   m.get().getModule().getChapter().getCourse().getCreator();
 if(!cg.equals(creator)) {
	 throw new IllegalAccessException("this modulecontent is not of you does not exist");
 }
 this.moduleContentRepository.deleteById(id);
 return;
}
//
@Transactional
public void deleteModuleContentByType(String email, 
                                      long courseId, 
                                      long chapterId, 
                                      long moduleId, 
                                      TypeOfContent type) 
                                      throws IllegalAccessException {
    
    // 1️⃣ Tìm customer
    Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    // 2️⃣ Kiểm tra creator
    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }

    // 3️⃣ Kiểm tra course có thuộc về creator không
    Course course = creator.getCourses().stream()
            .filter(c -> c.getCourseId() == courseId)
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException("Course not found or not owned by this creator"));

    // 4️⃣ Kiểm tra chapter trong course
    Chapter chapter = course.getChapters().stream()
            .filter(ch -> ch.getChapterId() == chapterId)
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException("Chapter not found in this course"));

    // 5️⃣ Kiểm tra module trong chapter
    Module module = chapter.getModules().stream()
            .filter(m -> m.getModuleId() == moduleId)
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException("Module not found in this chapter"));

    // 6️⃣ Xóa tất cả module content theo type
    moduleContentRepository.deleteByTypeOfContentAndModule(type,module);
}

//
public Chapter createChapter(String chapterName, long courseId, String email) throws IllegalAccessException {
    // 1. Kiểm tra customer tồn tại
    Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    // 2. Kiểm tra creator
    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }

    // 3. Tìm module theo moduleId trong tất cả các course của creator
    
    // 4. Tạo chapter mới
    Chapter chapter = new Chapter();
    chapter.setChapterName(chapterName);
    Course course = creator.getCourses().stream()
            .filter(c -> c.getCourseId() ==courseId)
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException("Course not found"));


    // 5. Thêm chapter vào course chứa module đó
   
chapter.setCourse(course);
    // 6. Lưu chapter
    return this.chapterRepository.save(chapter);
}

//
public Module createModule(ModuleDto moduleDto, String email) throws IllegalAccessException {
    // 1. Kiểm tra customer
    Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    // 2. Kiểm tra creator
    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }

    // 3. Tìm course
    Course course = creator.getCourses().stream()
            .filter(c -> c.getCourseId() == moduleDto.getCourseId())
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException("Course not found"));

    // 4. Tìm chapter
    Chapter chapter = course.getChapters().stream()
            .filter(ch -> ch.getChapterId() == moduleDto.getChapterId())
            .findFirst()
            .orElseThrow(() -> new IllegalAccessException("Chapter not found"));

    // 5. Tạo module mới
    Module module = new Module();
   // nếu muốn set manual, hoặc để auto generate
    module.setChapter(chapter);
    module.setTitleOfModule(moduleDto.getTitle());
    // bạn có thể thay bằng input từ client
  

    // 6. Thêm module vào chapter
   

    // 7. Lưu module
    return this.moduleRepository.save(module);
}
@Transactional
public void deleteModule(long moduleId, String email) throws IllegalAccessException {
    Customer customer = customerRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }

    // Tìm module
    Module module = creator.getCourses().stream()
        .flatMap(course -> course.getChapters().stream())
        .flatMap(chapter -> chapter.getModules().stream())
        .filter(m -> m.getModuleId() == moduleId)
        .findFirst()
        .orElseThrow(() -> new IllegalAccessException("Module not found"));

    // ✅ BƯỚC 1: Xóa tất cả ModuleContent trước
    moduleContentRepository.deleteByModuleId(module.getModuleId());
    // Hoặc nếu có method khác:
    // moduleContentRepository.deleteByModuleModuleId(moduleId);
    
    // ✅ BƯỚC 2: Flush để đảm bảo ModuleContent đã bị xóa
   
    
    // ✅ BƯỚC 3: Xóa Module
    moduleRepository.deleteByModuleId(moduleId);
}

@Transactional
public void deleteChapter(long chapterId, String email) throws IllegalAccessException {
    Customer customer = customerRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalAccessException("This account does not exist"));

    Creator creator = customer.getCreator();
    if (creator == null) {
        throw new IllegalAccessException("Creator does not exist");
    }

    // Tìm chapter
    Chapter chapter = creator.getCourses().stream()
        .flatMap(course -> course.getChapters().stream())
        .filter(ch -> ch.getChapterId() == chapterId)
        .findFirst()
        .orElseThrow(() -> new IllegalAccessException("Chapter not found"));

    // ✅ BƯỚC 1: Lấy tất cả modules trong chapter
    List<Module> modules = chapter.getModules();
    
    // ✅ BƯỚC 2: Xóa tất cả ModuleContent của từng Module
    for (Module module : modules) {
        moduleContentRepository.deleteByModuleId(module.getModuleId());
        // Hoặc: moduleContentRepository.deleteByModuleModuleId(module.getModuleId());
    }
    
    // ✅ BƯỚC 3: Flush để đảm bảo ModuleContent đã bị xóa
  
    
    // ✅ BƯỚC 4: Xóa tất cả Module
    moduleRepository.deleteByChapterChapterId(chapterId);
    
    // ✅ BƯỚC 5: Flush lại
   
    
    // ✅ BƯỚC 6: Xóa Chapter
    chapterRepository.deleteByChapterId(chapterId);
}


}
