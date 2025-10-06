package com.jpd.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jpd.web.dto.CourseCardDto;
import com.jpd.web.dto.CourseFormDto;
import com.jpd.web.dto.GenerateFeedbackForm;
import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.dto.ModuleDto;
import com.jpd.web.model.Chapter;
import com.jpd.web.model.Course;
import com.jpd.web.model.Module;
import com.jpd.web.model.ModuleContent;
import com.jpd.web.model.TypeOfContent;
import com.jpd.web.model.TypeOfFile;
import com.jpd.web.service.CourseService;
import com.jpd.web.service.FireBaseService;

import jakarta.validation.Valid;
import lombok.Builder;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/course")
public class CourseController {
@Autowired
private CourseService courseService;
@PostMapping(value =  "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> createCourse(@Valid @ModelAttribute  CourseFormDto entity,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
    Course c;
	try {
		c = this.courseService.createCourse(entity, jwt.getClaimAsString("email"));
		 return ResponseEntity.status(HttpStatus.OK).body(c);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		 return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
   
}
@GetMapping("/retrieveByEmail")
public ResponseEntity<List<CourseCardDto>>retrieveAll(@AuthenticationPrincipal Jwt jwt) {
    try {
    List<CourseCardDto>courses=	this.courseService.retrieveCourseByemail(jwt.getClaimAsString("email"));
    return ResponseEntity.status(HttpStatus.OK).body(courses);
    }
    catch (Exception e) {
		// TODO: handle exception
    	System.out.print(e);
    	return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    	
	}
}
@GetMapping("/getCourseContent")
public ResponseEntity<?> retrieveCourseById(@RequestParam("id") long id,@AuthenticationPrincipal Jwt jwt) {
    try {
    	String email=jwt.getClaimAsString("email");
    	return ResponseEntity.status(HttpStatus.OK).body(this.courseService.getCourseById(id, email));
    }
    catch (Exception e) {
		// TODO: handle exception
    	System.out.print(e);
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	}
	

}
@PostMapping("/generateFeeback")
public ResponseEntity<?> generateFeedback( @RequestBody GenerateFeedbackForm form ,@AuthenticationPrincipal
		Jwt jwt) throws IllegalAccessException{
	String email=jwt.getClaimAsString("email");
	String feedBack=this.courseService.generateFeedback(form.getQuestion(), form.getAnswer(),email);
	
	return ResponseEntity.status(HttpStatus.OK).body(feedBack);
}

@PostMapping("/saveImg")
public ResponseEntity<?> postMethodName(  @RequestParam("img") MultipartFile img ,@AuthenticationPrincipal Jwt jwt) throws IllegalAccessException {
 try {
	String email=jwt.getClaimAsString("email");
   String a=this.courseService.saveImgIntoFirebase(img, email,TypeOfFile.IMG);
   if(a==null) {return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();}
   else return ResponseEntity.status(HttpStatus.OK).body(a);
 }
 catch (Exception e) {
	// TODO: handle exception
	 e.printStackTrace();
	 return null;
}
}
 @PostMapping("/savePdf")
 public ResponseEntity<?> storePDf(  @RequestParam("pdf") MultipartFile pdf ,@AuthenticationPrincipal Jwt jwt) throws IllegalAccessException {
  try {
 	String email=jwt.getClaimAsString("email");
    String a=this.courseService.saveImgIntoFirebase(pdf, email,TypeOfFile.PDF);
    if(a==null) {return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();}
    else return ResponseEntity.status(HttpStatus.OK).body(a);
  }
  catch (Exception e) {
 	// TODO: handle exception
 	 e.printStackTrace();
 	 return null;
 }
}
@PostMapping("/update_course")
public ResponseEntity<?> updateCourseMaterial(@RequestBody ModuleContentDto content,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	String email=jwt.getClaimAsString("email");
	System.out.println(content.getModuleContent().get(0).getMcId());
    try {
		List<ModuleContent>mds= this.courseService.updateCourseMaterial(content, email);
		System.out.print(mds.size());
		return ResponseEntity.status(HttpStatus.OK).body(mds);
	} catch (IllegalAccessException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
    
}
@PostMapping("/createChapter")
public ResponseEntity<?> createChapter(@RequestParam ("chapterName") String name,@RequestParam("courseId") long courseId,@AuthenticationPrincipal
		 Jwt jwt) {
	
    //TODO: process POST request
    String email=jwt.getClaimAsString("email");
    Chapter chapter;
	try {
		chapter = this.courseService.createChapter(name, courseId, email);
		 return ResponseEntity.status(HttpStatus.OK).body(chapter);
	} catch (IllegalAccessException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		  return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
  
}
@PostMapping("/createModule")
public ResponseEntity<?> createModule(@RequestBody ModuleDto moduleDtom,@AuthenticationPrincipal Jwt jwt) {
    //TODO: process POST request
	 String email=jwt.getClaimAsString("email");
	 Module m;
	try {
		m = this.courseService.createModule(moduleDtom, email);
		return ResponseEntity.status(HttpStatus.OK).body(m);
	} catch (IllegalAccessException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}
  
}

public CourseController() {
	// TODO Auto-generated constructor stub
}

@DeleteMapping("/deleteModule/{moduleId}")
public ResponseEntity<?> deleteModule(@PathVariable long moduleId,
                                      @AuthenticationPrincipal Jwt jwt) {
    String email = jwt.getClaimAsString("email");
  
    try {
        courseService.deleteModule(moduleId, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content
    } catch (IllegalAccessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}

// --- DELETE CHAPTER ---
@DeleteMapping("/deleteChapter/{chapterId}")
public ResponseEntity<?> deleteChapter(@PathVariable long chapterId,
                                       @AuthenticationPrincipal Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    System.out.println("dllele");
    try {
        courseService.deleteChapter(chapterId, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content
    } catch (IllegalAccessException e) {
    	e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
@DeleteMapping("/deleteModuleContent/{moduleContentId}")
public ResponseEntity<?> deleteModuleContent(@PathVariable long moduleContentId,
                                       @AuthenticationPrincipal Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    System.out.println("dllele");
    try {
        courseService.deleteModuleContent( email,moduleContentId);
        return ResponseEntity.status(HttpStatus.OK).build(); // 204 No Content
    } catch (IllegalAccessException e) {
    	e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}

@DeleteMapping("/deleteModuleContentByType")
public ResponseEntity<?> deleteModuleContentByType(@RequestParam("type") TypeOfContent type,
		                              @RequestParam("moduleId")long moduleId,
		                              @RequestParam("chapterId")long chapterId,
		                              @RequestParam("courseId")long courseId,
                                       @AuthenticationPrincipal Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    System.out.println("dllele");
    try {
        courseService.deleteModuleContentByType( email,courseId,chapterId,moduleId,type);
        return ResponseEntity.status(HttpStatus.OK).build(); // 204 No Content
    } catch (IllegalAccessException e) {
    	e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
}
