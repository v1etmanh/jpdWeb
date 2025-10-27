# Prompt Log


## 2025-10-25 — ID: 2025-10-25-001

## Title:  for create course and update status
**Prompt:**

**Phân tích**

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
	        
	        this.moduleContentRepository.deleteAllById(idsToDelete);
	        this.moduleContentRepository.flush();
	         // ⚠️ Quan trọng: Force delete ngay
	    }
	    this.entityManager.clear();

//

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
		System.out.print(m.get());
		if(m.isPresent())
		 res.add(m.get());
		}
		return res;

		}

}


For each function, identify:
1. Main functionality
2. Input parameters and types
3. Expected return values
4. Potential edge cases
5. Dependencies that need mocking

**Response**

1) updateCourseMaterial(ModuleContentDto moduleContentDto, long creatorId)

Main functionality

Validates the creator’s ownership of the target Module (via ValidationResources.validateCompleteOwnership using module/chapter/course IDs from the DTO).

Diff-like behavior: partitions incoming ModuleContent list into toInsert and idsToDelete, deletes removed items, then bulk-saves new/updated ones (saveAll(toInsert)), flushing/clearing around deletes to ensure DB state consistency.

Inputs & types

moduleContentDto : ModuleContentDto

Must contain getModuleId(), getChapterId(), getCourseId(), and getModuleContent() returning List<ModuleContent>

creatorId : long

Returns

List<ModuleContent>: whatever saveAll(toInsert) returns (usually the persisted entities with IDs)

Key edge cases to test

DTO has empty/null moduleContent list (should do nothing or only deletes—verify behavior)

No-op diff (incoming equals existing → neither delete nor insert)

Only inserts (new contents all without IDs)

Only deletes (existing IDs present in DB but removed from DTO). Confirm delete order + flush + entityManager.clear() usage doesn’t break subsequent operations.

Mixed inserts/deletes

Ownership violation → validateCompleteOwnership should throw (e.g., wrong module/chapter/course/creator)

Dangling IDs in DTO (e.g., refers to content not belonging to the module) – how does the partitioning treat them? (If your missing middle block enforces this, test it; if not, this is a potential bug.)

Repository failures (e.g., saveAll throws, delete fails) – verify exceptions bubble up appropriately

Concurrency: two updates racing—ensure deletes + inserts maintain consistency (mock repository interactions to simulate interleaving exceptions)

Dependencies to mock

ValidationResources.validateCompleteOwnership(...)

ModuleContentRepository

Likely saveAll(...), deleteAllById(...), flush(), and possibly any reads used in the missing partition logic

EntityManager (clear())

2) deleteModuleContent(Long contentId, Long moduleId, Long chapterId, Long courseId, Long creatorId)

Main functionality

Validates full ownership (module/chapter/course + creator)

Loads ModuleContent by contentId, throws ModuleContentNotFoundException if absent

Ensures the content belongs to the specified module; if not, throws UnauthorizedException

Deletes the content by ID

Inputs & types

contentId, moduleId, chapterId, courseId, creatorId : Long

Returns

void (side effect is deletion)

Key edge cases to test

Content does not exist → expect ModuleContentNotFoundException

Content exists but belongs to a different module → expect UnauthorizedException

Ownership validation fails → the validateCompleteOwnership(...) should throw before deleting

Successful delete path

Repository exception during deleteById(...) → verify propagation

Dependencies to mock

ValidationResources.validateCompleteOwnership(...)

ModuleContentRepository.findById(...), deleteById(...)

3) deleteModuleContentsByType(TypeOfContent type, Long moduleId, Long chapterId, Long courseId, Long creatorId)

Main functionality

Validates full ownership

Bulk-deletes all ModuleContent of a given TypeOfContent in the module via deleteByTypeOfContentAndModule(...)

Inputs & types

type : TypeOfContent

moduleId, chapterId, courseId, creatorId : Long

Returns

void

Key edge cases to test

Ownership validation fails → ensure no deletion occurs

No content of that type → method should still succeed (idempotent bulk delete)

Repository failure on bulk delete → exception propagates

Null/invalid type (if not validated by controller layer), decide expected behavior (likely throw or no-op)

Dependencies to mock

ValidationResources.validateCompleteOwnership(...)

ModuleContentRepository.deleteByTypeOfContentAndModule(...)

4) getModuleContentsByTypeAndModuleId(..., Long moduleId, Long chapterId, Long courseId, Long creatorId)

Note: This method is truncated in the file. I can see:

It validates ownership

Calls findByTypeOfContentAndModule(type, module)

Then oddly re-fetches each entry by ID with findById(md.getMcId()) and builds a res list

Main functionality (inferred)

Return all contents of a specific TypeOfContent within a module that the creator fully owns

Possibly reloading each entity to ensure fully-initialized state (or to apply an optional filter)

Inputs & types (inferred)

type : TypeOfContent (missing in signature due to truncation, but referenced in body)

moduleId, chapterId, courseId, creatorId : Long

Returns

List<ModuleContent>

Key edge cases to test

Ownership validation fails

No results → returns empty list

Repository returns entities but findById later returns empty for some IDs → current code adds only isPresent(), so missing ones are silently skipped (verify that’s intended)

N+1 inefficiency: the re-fetch loop; ensure behavior is consistent if the first query returns proxies vs fully loaded entities

Repository failure during either the list fetch or the per-item fetch

Dependencies to mock

ValidationResources.validateCompleteOwnership(...)

ModuleContentRepository.findByTypeOfContentAndModule(...), findById(...)


**Prompt 2: Generate Test Cases**
Create comprehensive unit test cases for  functions in ModuleContentService class you just analyzed
aboveInclude: - Happy path scenarios
- Edge cases (boundary values)
- Error scenarios
- Integration with cart state
  Example :
  Category Test Case Input Expected Happy Path Add new item product={id:1, price:100}, qty=2 items.length = 1 Happy Path Add existing item Same product, qty=3 quantity updated to 5 Edge Case Quantity = 0 product={...}, qty=0 throws Error Edge Case Negative quantity product={...}, qty=-5 throws Error Error Null product product=null, qty=1 throws Error Error Undefined product product=undefined, qty=1 throws Error
  Using Given-When-Then pattern to structure test cases

**Response:**
**updateCourseMaterial(dto, creatorId)**

| Category   | Test Case                                   | Given (setup)                                         | When (action)                     | Input (key CSV cols)                                    | Then (expected)                                                                               |
| ---------- | ------------------------------------------- | ----------------------------------------------------- | --------------------------------- | ------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Happy Path | Create only new contents                    | Ownership is valid; repo empty                        | Call `updateCourseMaterial`       | `contentCount=3`, `hasExistingContent=false`, valid ids | `saveAll` called with 3 items; each new item gets ID; result size = 3; **no** `deleteAllById` |
| Happy Path | Replace mixed set (half existing, half new) | Ownership is valid; module currently has some content | Call `updateCourseMaterial`       | `contentCount=4`, `hasExistingContent=true`             | `saveAll` with 4 items; `deleteAllById` called for removed ones; result size = 4              |
| Edge       | Empty list treated as no-op                 | Ownership is valid                                    | Call with empty list              | `contentCount=0`                                        | Returns empty list; no `deleteAllById`                                                        |
| Edge       | New content with `null` ID                  | Ownership is valid                                    | Call with one content `mcId=null` | `contentCount=1`, `hasExistingContent=false`            | Treated as new; saved; no deletions                                                           |
| Edge       | New content with **negative** ID            | Ownership is valid                                    | Call with one content `mcId=-1`   | `contentCount=1`, `hasExistingContent=false`            | Treated as new; saved; no deletions                                                           |
| Error      | Module not found                            | Ownership validation fails                            | Call service                      | `expectedErrorType=ModuleNotFoundException`             | Throws `ModuleNotFoundException`; message contains module id                                  |
| Error      | Chapter not found                           | Ownership validation fails                            | Call service                      | `expectedErrorType=ChapterNotFoundException`            | Throws `ChapterNotFoundException`                                                             |
| Error      | Course not found                            | Ownership validation fails                            | Call service                      | `expectedErrorType=CourseNotFoundException`             | Throws `CourseNotFoundException`                                                              |
| Error      | Creator not found                           | Ownership validation fails                            | Call service                      | `expectedErrorType=CreatorNotFoundException`            | Throws `CreatorNotFoundException`                                                             |
| Error      | Unauthorized                                | Ownership validation fails                            | Call service                      | `expectedErrorType=UnauthorizedException`               | Throws `UnauthorizedException`                                                                |
| Error      | Module not in Chapter                       | Ownership validation fails                            | Call service                      | `expectedErrorType=ModuleNotBelongsToChapterException`  | Throws that exception                                                                         |
| Error      | Chapter not in Course                       | Ownership validation fails                            | Call service                      | `expectedErrorType=ChapterNotBelongsToCourseException`  | Throws that exception                                                                         |


**deleteModuleContent(contentId, moduleId, chapterId, courseId, creatorId)**

| Category   | Test Case                                      | Given (setup)                                                   | When        | Input                                       | Then                                                              |
| ---------- | ---------------------------------------------- | --------------------------------------------------------------- | ----------- | ------------------------------------------- | ----------------------------------------------------------------- |
| Happy Path | Delete existing content belonging to module    | Ownership valid; `findById` returns content with `moduleId`     | Call delete | Valid ids, `contentBelongsToModule=true`    | `deleteById(contentId)`; no exceptions                            |
| Edge       | Content absent (ID not found)                  | Ownership valid; `findById` returns `Optional.empty()`          | Call delete | `contentId=999` sentinel                    | Expect `ModuleContentNotFoundException` (or your chosen error)    |
| Edge       | Content exists but belongs to different module | Ownership valid; `findById` returns content with another module | Call delete | `contentBelongsToModule=false`              | Throws `ModuleContentNotFoundException` (or domain error you use) |
| Error      | Ownership invalid – module not found           | Validation throws                                               | Call delete | `expectedErrorType=ModuleNotFoundException` | Throws `ModuleNotFoundException`                                  |
| Error      | Unauthorized                                   | Validation throws                                               | Call delete | `expectedErrorType=UnauthorizedException`   | Throws `UnauthorizedException`                                    |

**deleteModuleContentsByType(type, moduleId, chapterId, courseId, creatorId)**

| Category   | Test Case              | Given                                    | When                          | Input                                                  | Then                                                                                      |
| ---------- | ---------------------- | ---------------------------------------- | ----------------------------- | ------------------------------------------------------ | ----------------------------------------------------------------------------------------- |
| Happy Path | Fetch 3 VIDEO contents | Ownership valid; repo returns 3 contents | Call get                      | `contentType=VIDEO`, `expectedCount=3`                 | Returns 3; `findByTypeOfContentAndModule` called; optional per-item `findById` stubs used |
| Edge       | No contents for type   | Ownership valid; repo returns empty      | Call get                      | `expectedCount=0`                                      | Returns empty list                                                                        |
| Error      | Ownership invalid      | Validation throws                        | Call get                      | `expectedErrorType=UnauthorizedException` (or similar) | Throws                                                                                    |
| Error      | Invalid enum in CSV    | CSV value bad                            | Test harness enum parse fails | —                                                      | Row skipped (as coded)                                                                    |





**Prompt 3: Generate  Test Code**

Create unit tests for the function in class ModuleContent Service  whose comprehensive unit test cases(Test Cases Matrix) you just analyzed above Requirements:
- Include setup/teardown - Use proper assertions (toEqual, toThrow)
- Add descriptive test names
- Mock any external dependencies . file csv tranfer to function test! not using mock data in function just use date from csv
- my project using spring boot,Junit

**Response:**

package com.jpd.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import com.jpd.web.model.Module;

import com.jpd.web.dto.ModuleContentDto;
import com.jpd.web.exception.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import com.jpd.web.service.ModuleContentService;
import com.jpd.web.service.utils.ValidationResources;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModuleContentService Tests")
class ModuleContentServiceTest {
static class TestModuleContent extends ModuleContent {
// Không cần thêm gì: Lombok @Data ở ModuleContent đã sinh getter/setter
}

    @Mock
    private ModuleContentRepository moduleContentRepository;

    @Mock
    private ValidationResources validationResources;

    @Mock
    private ReadingQuestionRepository readingQuestionRepository;

    @Mock
    private PassageRepository passageRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ModuleContentService moduleContentService;

    private Module mockModule;
    private Chapter mockChapter;
    private Course mockCourse;
    private Creator mockCreator;

    @BeforeEach
    void setUp() {
        // Setup mock entities without stubbing
        mockCreator = new Creator();
        mockCreator.setCreatorId(1L);

        mockCourse = new Course();
        mockCourse.setCourseId(1L);
        mockCourse.setCreator(mockCreator);

        mockChapter = new Chapter();
        mockChapter.setChapterId(1L);
        mockChapter.setCourse(mockCourse);

        mockModule = new Module();
        mockModule.setModuleId(1L);
        mockModule.setChapter(mockChapter);
    }

    /**
     * ✅ FIXED: Create real ModuleContent instead of mock
     * Giả sử ModuleContent KHÔNG phải abstract class
     */
    private ModuleContent createModuleContent(Long id, TypeOfContent type, Module module) {
        TestModuleContent content = new TestModuleContent();
        content.setMcId(id);
        content.setTypeOfContent(type);
        content.setModule(module);
        return content;
    }

    /**
     * Helper method to read CSV test data
     */
    private List<Map<String, String>> readTestDataFromCSV(String fileName) throws IOException {
        List<Map<String, String>> testData = new ArrayList<>();

        System.out.println("=== Reading CSV file: " + fileName + " ===");

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(fileName).getInputStream()))) {

            String headerLine = br.readLine();
            if (headerLine == null) {
                System.err.println("WARNING: Empty CSV file: " + fileName);
                return testData;
            }

            String[] headers = headerLine.split(",");
            System.out.println("Headers found: " + String.join(", ", headers));

            String line;
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }

                String[] values = line.split(",", -1);
                Map<String, String> row = new HashMap<>();

                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i].trim();
                    String value = i < values.length ? values[i].trim() : "";
                    row.put(header, value);
                }

                System.out.println("Line " + lineNumber + " parsed: " + row);
                testData.add(row);
            }
        } catch (Exception e) {
            System.err.println("ERROR reading CSV file: " + fileName);
            e.printStackTrace();
            throw e;
        }

        System.out.println("Total rows parsed from " + fileName + ": " + testData.size());
        System.out.println("===========================================\n");

        return testData;
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nested
    @DisplayName("updateCourseMaterial() Tests - Data Driven")
    class UpdateCourseMaterialTestsFromCSV {

        @TestFactory
        @DisplayName("Run all update course material test cases from CSV")
        List<DynamicTest> testUpdateCourseMaterialFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-update-course-material.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");
                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeUpdateCourseMaterialTest(data);
                });
                dynamicTests.add(test);
            }

            return dynamicTests;
        }
        private void executeUpdateCourseMaterialTest(Map<String, String> data) {
            // Reset mocks để tránh UnnecessaryStubbingException
            reset(moduleContentRepository, validationResources, entityManager);

            // Parse test data
            Long moduleId = parseLongSafe(data.get("moduleId"));
            Long chapterId = parseLongSafe(data.get("chapterId"));
            Long courseId = parseLongSafe(data.get("courseId"));
            Long creatorId = parseLongSafe(data.get("creatorId"));
            Integer contentCount = parseIntSafe(data.get("contentCount"));
            boolean hasExistingContent = Boolean.parseBoolean(data.get("hasExistingContent"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.getOrDefault("expectedErrorType", "");
            String expectedErrorMessage = data.getOrDefault("expectedErrorMessage", "");

            if (moduleId == null || chapterId == null || courseId == null || creatorId == null) {
                System.err.println("WARNING: Skipping invalid row: " + data);
                return;
            }

            // Setup test entities
            Module testModule = new Module();
            testModule.setModuleId(moduleId);

            // Create module content DTO
            ModuleContentDto dto = new ModuleContentDto();
            dto.setModuleId(moduleId);
            dto.setChapterId(chapterId);
            dto.setCourseId(courseId);

            // ✅ FIXED: Create REAL ModuleContent objects
            List<ModuleContent> contents = new ArrayList<>();
            for (int i = 0; i < (contentCount != null ? contentCount : 0); i++) {
                Long contentId = hasExistingContent && i < contentCount / 2 ? (long) (i + 1) : null;
                ModuleContent content = createModuleContent(contentId, TypeOfContent.VIDEO, testModule);
                contents.add(content);
            }
            dto.setModuleContent(contents);

            // Setup mocks ONLY for scenarios that will be used
            if (shouldSucceed) {
                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenReturn(testModule);

                when(moduleContentRepository.saveAll(anyList()))
                        .thenAnswer(invocation -> {
                            List<ModuleContent> toSave = invocation.getArgument(0);
                            // Simulate ID generation for new contents
                            for (int i = 0; i < toSave.size(); i++) {
                                if (toSave.get(i).getMcId() == null) {
                                    toSave.get(i).setMcId((long) (i + 100));
                                }
                            }
                            return toSave;
                        });

                doNothing().when(moduleContentRepository).deleteAllById(anyList());
                doNothing().when(moduleContentRepository).flush();
                doNothing().when(entityManager).clear();

                // Execute and verify
                List<ModuleContent> result = moduleContentService.updateCourseMaterial(dto, creatorId);

                assertNotNull(result);
                assertEquals(contentCount, result.size());

                verify(validationResources).validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
                verify(moduleContentRepository).saveAll(anyList());

                if (hasExistingContent && contentCount > 0) {
                    verify(moduleContentRepository).deleteAllById(anyList());
                }
            } else {
                // Setup for failure scenarios
                Exception exception = createException(expectedErrorType, moduleId, chapterId, courseId, creatorId);
                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenThrow(exception);

                // Execute and verify exception
                Exception thrown = assertThrows(Exception.class,
                        () -> moduleContentService.updateCourseMaterial(dto, creatorId));

                String actualExceptionType = thrown.getClass().getSimpleName();
                assertTrue(actualExceptionType.contains(expectedErrorType.replace("Exception", ""))
                                || actualExceptionType.equals(expectedErrorType),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(thrown.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage +
                                    "' but got: '" + thrown.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("deleteModuleContent() Tests - Data Driven")
    class DeleteModuleContentTestsFromCSV {

        @TestFactory
        @DisplayName("Run all delete module content test cases from CSV")
        List<DynamicTest> testDeleteModuleContentFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-delete-module-content.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");
                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeDeleteModuleContentTest(data);
                });
                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeDeleteModuleContentTest(Map<String, String> data) {
            reset(moduleContentRepository, validationResources);

            Long contentId = parseLongSafe(data.get("contentId"));
            Long moduleId = parseLongSafe(data.get("moduleId"));
            Long chapterId = parseLongSafe(data.get("chapterId"));
            Long courseId = parseLongSafe(data.get("courseId"));
            Long creatorId = parseLongSafe(data.get("creatorId"));
            boolean contentBelongsToModule = Boolean.parseBoolean(data.get("contentBelongsToModule"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.getOrDefault("expectedErrorType", "");
            String expectedErrorMessage = data.getOrDefault("expectedErrorMessage", "");

            if (contentId == null || moduleId == null || chapterId == null ||
                    courseId == null || creatorId == null) {
                System.err.println("WARNING: Skipping invalid row: " + data);
                return;
            }

            // Setup test entities
            Module testModule = new Module();
            testModule.setModuleId(contentBelongsToModule ? moduleId : moduleId + 100);

            ModuleContent testContent = createModuleContent(contentId, TypeOfContent.VIDEO, testModule);

            // Setup mocks based on scenario
            if (contentId == 999L) {
                when(moduleContentRepository.findById(contentId)).thenReturn(Optional.empty());
            } else {
                when(moduleContentRepository.findById(contentId)).thenReturn(Optional.of(testContent));
            }

            if (shouldSucceed) {
                Module ownerModule = new Module();
                ownerModule.setModuleId(moduleId);

                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenReturn(ownerModule);
                doNothing().when(moduleContentRepository).deleteById(contentId);

                // Execute
                assertDoesNotThrow(() ->
                        moduleContentService.deleteModuleContent(contentId, moduleId, chapterId, courseId, creatorId)
                );

                verify(validationResources).validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
                verify(moduleContentRepository).findById(contentId);
                verify(moduleContentRepository).deleteById(contentId);
            } else {
                if (contentId != 999L && !contentBelongsToModule) {
                    Module ownerModule = new Module();
                    ownerModule.setModuleId(moduleId);
                    when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                            .thenReturn(ownerModule);
                } else {
                    Exception exception = createException(expectedErrorType, moduleId, chapterId, courseId, creatorId);
                    when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                            .thenThrow(exception);
                }
// Execute and verify exception
Exception thrown = assertThrows(Exception.class, () ->
moduleContentService.deleteModuleContent(contentId, moduleId, chapterId, courseId, creatorId)
);

                String actualExceptionType = thrown.getClass().getSimpleName();
                assertTrue(actualExceptionType.contains(expectedErrorType.replace("Exception", ""))
                                || actualExceptionType.equals(expectedErrorType),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(thrown.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage +
                                    "' but got: '" + thrown.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("deleteModuleContentsByType() Tests - Data Driven")
    class DeleteByTypeTestsFromCSV {

        @TestFactory
        @DisplayName("Run all delete by type test cases from CSV")
        List<DynamicTest> testDeleteByTypeFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-delete-by-type.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");
                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeDeleteByTypeTest(data);
                });
                dynamicTests.add(test);
            }

            return dynamicTests;
        }

        private void executeDeleteByTypeTest(Map<String, String> data) {
            reset(moduleContentRepository, validationResources);

            String contentTypeStr = data.get("contentType");
            Long moduleId = parseLongSafe(data.get("moduleId"));
            Long chapterId = parseLongSafe(data.get("chapterId"));
            Long courseId = parseLongSafe(data.get("courseId"));
            Long creatorId = parseLongSafe(data.get("creatorId"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.getOrDefault("expectedErrorType", "");
            String expectedErrorMessage = data.getOrDefault("expectedErrorMessage", "");

            if (contentTypeStr == null || moduleId == null || chapterId == null ||
                    courseId == null || creatorId == null) {
                System.err.println("WARNING: Skipping invalid row: " + data);
                return;
            }

            TypeOfContent contentType;
            try {
                contentType = TypeOfContent.valueOf(contentTypeStr);
            } catch (IllegalArgumentException e) {
                System.err.println("WARNING: Invalid content type: " + contentTypeStr);
                return;
            }

            Module testModule = new Module();
            testModule.setModuleId(moduleId);

            if (shouldSucceed) {
                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenReturn(testModule);
                doNothing().when(moduleContentRepository).deleteByTypeOfContentAndModule(contentType, testModule);

                assertDoesNotThrow(() ->
                        moduleContentService.deleteModuleContentsByType(contentType, moduleId, chapterId, courseId, creatorId)
                );

                verify(validationResources).validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
                verify(moduleContentRepository).deleteByTypeOfContentAndModule(contentType, testModule);
            } else {
                Exception exception = createException(expectedErrorType, moduleId, chapterId, courseId, creatorId);
                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenThrow(exception);

                Exception thrown = assertThrows(Exception.class, () ->
                        moduleContentService.deleteModuleContentsByType(contentType, moduleId, chapterId, courseId, creatorId)
                );

                String actualExceptionType = thrown.getClass().getSimpleName();
                assertTrue(actualExceptionType.contains(expectedErrorType.replace("Exception", ""))
                                || actualExceptionType.equals(expectedErrorType),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(thrown.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage +
                                    "' but got: '" + thrown.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("getModuleContentsByTypeAndModuleId() Tests - Data Driven")
    class GetContentByTypeTestsFromCSV {

        @TestFactory
        @DisplayName("Run all get content by type test cases from CSV")
        List<DynamicTest> testGetContentByTypeFromCSV() throws IOException {
            List<Map<String, String>> testData = readTestDataFromCSV("test-data-get-content-by-type.csv");
            List<DynamicTest> dynamicTests = new ArrayList<>();

            for (Map<String, String> data : testData) {
                String testCase = data.get("testCase");
                DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                    executeGetContentByTypeTest(data);
                });
                dynamicTests.add(test);
            }

            return dynamicTests;
        }
        private void executeGetContentByTypeTest(Map<String, String> data) {
            reset(moduleContentRepository, validationResources);

            String contentTypeStr = data.get("contentType");
            Long moduleId = parseLongSafe(data.get("moduleId"));
            Long chapterId = parseLongSafe(data.get("chapterId"));
            Long courseId = parseLongSafe(data.get("courseId"));
            Long creatorId = parseLongSafe(data.get("creatorId"));
            Integer expectedCount = parseIntSafe(data.get("expectedCount"));
            boolean shouldSucceed = Boolean.parseBoolean(data.get("shouldSucceed"));
            String expectedErrorType = data.getOrDefault("expectedErrorType", "");
            String expectedErrorMessage = data.getOrDefault("expectedErrorMessage", "");

            if (contentTypeStr == null || moduleId == null || chapterId == null ||
                    courseId == null || creatorId == null) {
                System.err.println("WARNING: Skipping invalid row: " + data);
                return;
            }

            TypeOfContent contentType;
            try {
                contentType = TypeOfContent.valueOf(contentTypeStr);
            } catch (IllegalArgumentException e) {
                System.err.println("WARNING: Invalid content type: " + contentTypeStr);
                return;
            }

            Module testModule = new Module();
            testModule.setModuleId(moduleId);

            List<ModuleContent> mockContents = new ArrayList<>();
            for (int i = 0; i < (expectedCount != null ? expectedCount : 0); i++) {
                ModuleContent content = createModuleContent((long) (i + 1), contentType, testModule);
                mockContents.add(content);
            }

            if (shouldSucceed) {
                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenReturn(testModule);
                when(moduleContentRepository.findByTypeOfContentAndModule(contentType, testModule))
                        .thenReturn(mockContents);

                // Mock findById for each content
                for (ModuleContent content : mockContents) {
                    when(moduleContentRepository.findById(content.getMcId()))
                            .thenReturn(Optional.of(content));
                }

                List<ModuleContent> result = moduleContentService.getModuleContentsByTypeAndModuleId(
                        contentType, moduleId, chapterId, courseId, creatorId);

                assertNotNull(result);
                assertEquals(expectedCount, result.size());

                verify(validationResources).validateCompleteOwnership(moduleId, chapterId, courseId, creatorId);
                verify(moduleContentRepository).findByTypeOfContentAndModule(contentType, testModule);
            } else {
                Exception exception = createException(expectedErrorType, moduleId, chapterId, courseId, creatorId);
                when(validationResources.validateCompleteOwnership(moduleId, chapterId, courseId, creatorId))
                        .thenThrow(exception);

                Exception thrown = assertThrows(Exception.class, () ->
                        moduleContentService.getModuleContentsByTypeAndModuleId(
                                contentType, moduleId, chapterId, courseId, creatorId)
                );

                String actualExceptionType = thrown.getClass().getSimpleName();
                assertTrue(actualExceptionType.contains(expectedErrorType.replace("Exception", ""))
                                || actualExceptionType.equals(expectedErrorType),
                        "Expected " + expectedErrorType + " but got " + actualExceptionType);

                if (!expectedErrorMessage.isEmpty()) {
                    assertTrue(thrown.getMessage().contains(expectedErrorMessage),
                            "Expected message to contain: '" + expectedErrorMessage +
                                    "' but got: '" + thrown.getMessage() + "'");
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty module content list")
        void shouldHandleEmptyModuleContentList() {
            ModuleContentDto dto = new ModuleContentDto();
            dto.setModuleId(1L);
            dto.setChapterId(1L);
            dto.setCourseId(1L);
            dto.setModuleContent(new ArrayList<>());

            when(validationResources.validateCompleteOwnership(1L, 1L, 1L, 1L))
                    .thenReturn(mockModule);
            when(moduleContentRepository.saveAll(anyList()))
                    .thenReturn(new ArrayList<>());

            List<ModuleContent> result = moduleContentService.updateCourseMaterial(dto, 1L);

            assertNotNull(result);
            assertEquals(0, result.size());
            verify(moduleContentRepository, never()).deleteAllById(anyList());
        }

        @Test
        @DisplayName("Should handle content with null ID as new content")
        void shouldHandleContentWithNullIdAsNew() {
            ModuleContentDto dto = new ModuleContentDto();
            dto.setModuleId(1L);
            dto.setChapterId(1L);
            dto.setCourseId(1L);

            ModuleContent newContent = createModuleContent(null, TypeOfContent.VIDEO, mockModule);
            dto.setModuleContent(Arrays.asList(newContent));

            when(validationResources.validateCompleteOwnership(1L, 1L, 1L, 1L))
                    .thenReturn(mockModule);
            when(moduleContentRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<ModuleContent> result = moduleContentService.updateCourseMaterial(dto, 1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(moduleContentRepository, never()).deleteAllById(anyList());
        }

        @Test
        @DisplayName("Should handle content with negative ID as new content")
        void shouldHandleContentWithNegativeIdAsNew() {
            ModuleContentDto dto = new ModuleContentDto();
            dto.setModuleId(1L);
            dto.setChapterId(1L);
            dto.setCourseId(1L);

            ModuleContent newContent = createModuleContent(-1L, TypeOfContent.VIDEO, mockModule);
            dto.setModuleContent(Arrays.asList(newContent));

            when(validationResources.validateCompleteOwnership(1L, 1L, 1L, 1L))
                    .thenReturn(mockModule);
            when(moduleContentRepository.saveAll(anyList()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<ModuleContent> result = moduleContentService.updateCourseMaterial(dto, 1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(moduleContentRepository, never()).deleteAllById(anyList());
        }
    }

    private Exception createException(String exceptionType, Long moduleId, Long chapterId,
                                      Long courseId, Long creatorId) {
        switch (exceptionType) {
            case "ModuleNotFoundException":
                return new ModuleNotFoundException(moduleId);
            case "ChapterNotFoundException":
                return new ChapterNotFoundException(chapterId);
            case "CourseNotFoundException":
                return new CourseNotFoundException(courseId);
            case "CreatorNotFoundException":
                return new CreatorNotFoundException(creatorId);
            case "ModuleContentNotFoundException":
                return new ModuleContentNotFoundException(moduleId);
            case "UnauthorizedException":
                return new UnauthorizedException("Unauthorized access");
            case "ModuleNotBelongsToChapterException":
                return new ModuleNotBelongsToChapterException(moduleId, chapterId);
            case "ChapterNotBelongsToCourseException":
                return new ChapterNotBelongsToCourseException(chapterId, courseId);
            default:
                return new RuntimeException("Unknown exception type: " + exceptionType);
        }
    }
}