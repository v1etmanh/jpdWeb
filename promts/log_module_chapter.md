Prompt Log
2025-10-25 — ID: 2025-10-25-001
Title:  for module and chapter
Prompt:
Phân tích
Imagine you are Senior QA Engineer specializing in Java Spring Boot testing.
Analyze this Course management class and identify all functions that need unit testing:
ModuleController.java
@RestController
@RequestMapping("/api/creator/{courseId}/{chapterId}/module")
public class ModuleController {
@Autowired
private ModuleService moduleService;
@DeleteMapping("/{moduleId}")
public ResponseEntity<?> deleteModule(
@Positive(message = "Module ID must be positive") @PathVariable("moduleId") Long moduleId,
@Positive(message = "Chapter ID must be positive") @PathVariable("chapterId") Long chapterId,
@Positive(message = "Course ID must be positive") @PathVariable("courseId") Long courseId,
HttpServletRequest request) {
long creatorId = RequestAttributeExtractor.extractCreatorId(request);
moduleService.deleteModule(creatorId, courseId, chapterId, moduleId);
return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content
}
@PostMapping()
public ResponseEntity<?> createModule(@Valid @RequestParam("moduleName") String moduleName,
@Positive(message = "Course ID must be positive") @PathVariable("courseId") Long courseId,
@Positive(message = "Chapter ID must be positive") @PathVariable("chapterId") Long chapterId,
HttpServletRequest request) {
// TODO: process POST request
Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
// GlobalExceptionHandler sẽ xử lý exceptions
Module module= moduleService.createModule(moduleName, creatorId, courseId, chapterId);
return ResponseEntity.status(HttpStatus.CREATED).body(module);
}
@PutMapping("/{moduleId}/update")
public ResponseEntity<?> putMethodName(@PathVariable("moduleId") long moduleId, @RequestParam String name,
HttpServletRequest request) {
//TODO: process PUT request
Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
this.moduleService.updateModuleName(creatorId, moduleId, name);
return ResponseEntity.noContent().build();
}
}
ChapterController.java
@Slf4j
@Validated
@RestController
@RequestMapping("/api/creator/{courseId}/chapter")
@RequiredArgsConstructor
public class ChapterController {
private final ChapterService chapterService;
@PostMapping
public ResponseEntity<?> createChapter(
@NotBlank(message = "Chapter name is required")
@RequestParam("chapterName") String name,
@Positive(message = "Course ID must be positive")
@PathVariable("courseId") Long courseId,
HttpServletRequest request) {
log.info("Creating chapter '{}' for course {}", name, courseId);
Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
// ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
Chapter chapter = chapterService.createChapter(name, courseId, creatorId);
return ResponseEntity
.status(HttpStatus.CREATED)
.body(chapter);
}
@DeleteMapping("/{chapterId}")
public ResponseEntity<Void> deleteChapter(
@Positive(message = "Chapter ID must be positive")
@PathVariable("chapterId") Long chapterId,
@Positive(message = "Course ID must be positive")
@PathVariable("courseId") Long courseId,
HttpServletRequest request) {
log.info("Deleting chapter {} from course {}", chapterId, courseId);
Long creatorId =RequestAttributeExtractor.extractCreatorId(request);
// ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
chapterService.deleteChapter(chapterId, courseId, creatorId);
return ResponseEntity.noContent().build();
}
@PutMapping("/{chapterID}/update")
public ResponseEntity<?>updateChapter(@RequestParam String name ,HttpServletRequest request,
@PathVariable("chapterID")long chapterId){
System.out.print("d");
Long creatorId =RequestAttributeExtractor.extractCreatorId(request);
this.chapterService.updateChapter(creatorId, name, chapterId);
return ResponseEntity.noContent().build();
}
}
For each function, identify:

Main functionality
Input parameters and types
Expected return values
Potential edge cases
Dependencies that need mocking

Response:
Unit Testing Analysis for Course Management Controllers

ModuleController
1.1 deleteModule()
Main Functionality:
· Deletes a module from a specific chapter and course
· Validates ownership through creator authentication
Input Parameters:
· moduleId: Long (path variable, must be positive)
· chapterId: Long (path variable, must be positive)
· courseId: Long (path variable, must be positive)
· request: HttpServletRequest (for extracting creatorId)
Expected Return Values:
· HTTP 204 No Content on successful deletion
Potential Edge Cases:
Invalid moduleId (zero, negative, null)
Module doesn't exist
Chapter doesn't exist
Course doesn't exist
Creator doesn't own the course/chapter/module (authorization failure)
Module belongs to different chapter/course (mismatched IDs)
Request missing creator authentication attributes
Database constraint violations (e.g., cascade delete issues)
Dependencies to Mock:
· ModuleService (moduleService)
· RequestAttributeExtractor (static method)
· HttpServletRequest

1.2 createModule()
Main Functionality:
· Creates a new module within a specific chapter and course
· Validates module name and ownership
Input Parameters:
· moduleName: String (request param, must be valid/non-blank)
· courseId: Long (path variable, must be positive)
· chapterId: Long (path variable, must be positive)
· request: HttpServletRequest (for extracting creatorId)
Expected Return Values:
· HTTP 201 Created with Module object in response body
Potential Edge Cases:

Blank/null/empty module name
Module name exceeds max length
Duplicate module name within same chapter
Invalid courseId or chapterId
Chapter doesn't belong to specified course
Creator doesn't own the course
Special characters/SQL injection attempts in module name
Request missing creator authentication
Service returns null (should not happen but test defensive coding)
Dependencies to Mock:
· ModuleService (moduleService)
· RequestAttributeExtractor (static method)
· HttpServletRequest

1.3 putMethodName() (updateModuleName)
Main Functionality:
· Updates the name of an existing module
· Validates ownership and module existence
Input Parameters:
· moduleId: long (path variable, no validation annotation - potential bug!)
· name: String (request param, no validation - potential bug!)
· request: HttpServletRequest (for extracting creatorId)
Expected Return Values:
· HTTP 204 No Content on successful update
Potential Edge Cases:

Invalid moduleId (zero, negative)
Module doesn't exist
Blank/null/empty name
Name exceeds max length
Duplicate name within same chapter
Creator doesn't own the module
Request missing creator authentication
Concurrent update conflicts
Dependencies to Mock:
· ModuleService (moduleService)
· RequestAttributeExtractor (static method)
· HttpServletRequest
ChapterController
2.1 createChapter()
Main Functionality:
· Creates a new chapter within a course
· Validates chapter name and course ownership
Input Parameters:
· name: String (request param, must not be blank)
· courseId: Long (path variable, must be positive)
· request: HttpServletRequest (for extracting creatorId)
Expected Return Values:
· HTTP 201 Created with Chapter object in response body
Potential Edge Cases:
Blank/null/whitespace-only chapter name
Chapter name exceeds max length
Duplicate chapter name within same course
Invalid/non-existent courseId
Creator doesn't own the course
Special characters in chapter name
Request missing creator authentication
Service returns null
Database transaction failures
Dependencies to Mock:
· ChapterService (chapterService)
· RequestAttributeExtractor (s...

Prompt 2: Generate Test Cases for deleteModule()
Comprehensive Unit Test Cases for deleteModule()
Generate a full set of high-quality unit test for the deleteModule() operation in the Course Management domain. Focus on both Happy path scenarios and critical edge cases, ensuring Error scenarios, Integration with cart state
@DeleteMapping("/{moduleId}")
public ResponseEntity<?> deleteModule(
@Positive(message = "Module ID must be positive") @PathVariable("moduleId") Long moduleId,
@Positive(message = "Chapter ID must be positive") @PathVariable("chapterId") Long chapterId,
@Positive(message = "Course ID must be positive") @PathVariable("courseId") Long courseId,
HttpServletRequest request) {
long creatorId = RequestAttributeExtractor.extractCreatorId(request);
moduleService.deleteModule(creatorId, courseId, chapterId, moduleId);
return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content
}
Response:
Unit Test Suite: ModuleController.deleteModule()
Test Case Matrix:
Category
Test Case
Input
Expected Result
Happy Path
Delete module successfully
moduleId=1, chapterId=1, courseId=1, creatorId=100
HTTP 204 No Content, service.deleteModule() called
Happy Path
Delete with multiple modules present
moduleId=2 (middle module)
HTTP 204 No Content, other modules unaffected
Validation
Zero moduleId
moduleId=0, chapterId=1, courseId=1
Throws ValidationException
Validation
Negative moduleId
moduleId=-1, chapterId=1, courseId=1
Throws ValidationException
Validation
Zero chapterId
moduleId=1, chapterId=0, courseId=1
Throws ValidationException
Validation
Negative chapterId
moduleId=1, chapterId=-1, courseId=1
Throws ValidationException
Validation
Zero courseId
moduleId=1, chapterId=1, courseId=0
Throws ValidationException
Validation
Negative courseId
moduleId=1, chapterId=1, courseId=-1
Throws ValidationException
Edge Case
Module doesn't exist
moduleId=9999
Service throws ResourceNotFoundException
Edge Case
Chapter doesn't exist
chapterId=9999
Service throws ResourceNotFoundException
Edge Case
Chapter belongs to different course
chapterId=5 but not under courseId
Service throws ValidationException
Edge Case
SQL injection attempt
moduleName="'; DROP TABLE modules;--"
Safely escaped, module created
Authorization
Creator doesn’t own course
creatorId=999 (not owner)
Service throws UnauthorizedException
Authorization
Missing creator ID
Request without creatorId attribute
UnauthorizedException thrown
Error
Database constraint violation
Valid inputs backend but DB constraint fails
Service throws DatabaseException
Error
Transaction rollback scenario
Exception occurs mid-creation
No module created, exception propagated
Integration
Chapter module count updates
Create new module
Chapter's module count incremented
Integration
Course statistics auto update
Create new module
Course's totalModules incremented
Prompt 3: Generate Test Cases for putMethodName()
Comprehensive Unit Test Cases for putMethodName()
Generate a full set of high-quality unit test for the putMethodName() operation in the Course Management domain. Focus on both Happy path scenarios and critical edge cases, ensuring Error scenarios, Integration with cart state
@PutMapping("/{moduleId}/update")
public ResponseEntity<?> putMethodName(@PathVariable("moduleId") long moduleId, @RequestParam String name,
HttpServletRequest request) {
//TODO: process PUT request
Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
this.moduleService.updateModuleName(creatorId, moduleId, name);
return ResponseEntity.noContent().build();
}
Response:
Unit Test Suite: ModuleController.putMethodName()
Test Case Matrix:
Category
Test Case
Input
Expected Outcome
Happy Path
Update module name successfully
moduleId=1, name="Updated Module", creatorId=100
HTTP 204 No Content, service.updateModuleName() called
Happy Path
Update with long valid name
moduleId=1, name="Advanced Java..." (≈50 chars)
HTTP 204 No Content
Validation
Blank module name
moduleId=1, name=""
Service throws ValidationException
Validation
Null module name
moduleId=1, name=null
Service throws ValidationException
Validation
Whitespace-only name
moduleId=1, name=" "
Service throws ValidationException
Validation
Zero moduleId
moduleId=0, name="Valid"
Service throws ValidationException
Validation
Negative moduleId
moduleId=-5, name="Valid"
Service throws ValidationException
Edge Case
Module name too long
name=300-char string
Service throws ValidationException
Edge Case
Module doesn’t exist
moduleId=9999
Service throws ResourceNotFoundException
Edge Case
Duplicate module name
name="Intro" (already exists)
Service throws DuplicateResourceException
Edge Case
SQL injection attempt
name="'; DROP TABLE;--"
Properly escaped → HTTP 204 No Content
Authorization
Creator doesn’t own module
creatorId=999
Service throws UnauthorizedException
Authorization
Missing creator ID
No creatorId in request
UnauthorizedException thrown
Error
Concurrent update (race condition)
Valid update, but module locked/changed elsewhere
Service throws OptimisticLockException
Cart State
Module in active cart
Module is in active cart during update
HTTP 204, cart items synced/updated
Integration
Full valid flow
All params valid
HTTP 204, correct service method sequence called
Prompt 4: Generate Test Cases for createChapter()
Comprehensive Unit Test Cases for createChapter()
Generate a full set of high-quality unit test for the createChapter()operation in the Course Management domain. Focus on both Happy path scenarios and critical edge cases, ensuring Error scenarios, Integration with cart state
@PostMapping
public ResponseEntity<?> createChapter(
@NotBlank(message = "Chapter name is required")
@RequestParam("chapterName") String name,
@Positive(message = "Course ID must be positive")
@PathVariable("courseId") Long courseId,
HttpServletRequest request) {
log.info("Creating chapter '{}' for course {}", name, courseId);
Long creatorId = RequestAttributeExtractor.extractCreatorId(request);
// ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
Chapter chapter = chapterService.createChapter(name, courseId, creatorId);
return ResponseEntity
.status(HttpStatus.CREATED)
.body(chapter);
}
Response:
Unit Test Suite: ModuleController.createChapter()
Test Case Matrix:
Category
Test Case
Input
Expected Result
Happy Path
Create chapter successfully
name="Introduction to Java", courseId=1, creatorId=100
HttpStatus.CREATED (201), Chapter object returned
Happy Path
Create chapter with special characters
name="Chapter #1: C++ & Java", courseId=1, creatorId=100
HttpStatus.CREATED (201), Special chars preserved
Happy Path
Create chapter with minimum name
name="A", courseId=1, creatorId=100
HttpStatus.CREATED (201), Single char accepted
Edge Case
Blank chapter name
name="", courseId=1, creatorId=100
Throws validation error: "Chapter name is required"
Edge Case
Null chapter name
name=null, courseId=1, creatorId=100
Throws validation error: "Chapter name is required"
Edge Case
Whitespace-only name
name=" ", courseId=1, creatorId=100
Throws validation error: "Chapter name is required"
Edge Case
Zero course ID
name="Test", courseId=0, creatorId=100
Throws validation error: "Course ID must be positive"
Edge Case
Negative course ID
name="Test", courseId=-1, creatorId=100
Throws validation error: "Course ID must be positive"
Error
Course not found
name="Test", courseId=999, creatorId=100
Throws ResourceNotFoundException: "Course not found"
Error
Creator not authorized
name="Test", courseId=1, creatorId=999 (unauthorized)
Throws UnauthorizedException: "Not authorized"
Error
Duplicate chapter name
name="Introduction" (exists), courseId=1, creatorId=100
Throws DuplicateResourceException: "Chapter already exists"
Error
Creator ID not found in request
name="Test", courseId=1, request (no creator)
Throws UnauthorizedException: "Creator ID not found"
Error
Null HTTP request
name="Test", courseId=1, request=null
Throws IllegalArgumentException: "Request cannot be null"
Error
Service returns null
name="Test", courseId=1, service returns null
Throws NullPointerException or error response
Integration
Verify service called correctly
name="Test", courseId=1, creatorId=100
chapterService.createChapter() called once with correct params
Integration
Verify service NOT called on validation fail
name="", courseId=1, creatorId=100
chapterService.createChapter() never called
Prompt 5: Generate Test Cases for deleteChapter()
Comprehensive Unit Test Cases for deleteChapter()
Generate a full set of high-quality unit test for the deleteChapter()operation in the Course Management domain. Focus on both Happy path scenarios and critical edge cases, ensuring Error scenarios, Integration with cart state
@DeleteMapping("/{chapterId}")
public ResponseEntity<Void> deleteChapter(
@Positive(message = "Chapter ID must be positive")
@PathVariable("chapterId") Long chapterId,
@Positive(message = "Course ID must be positive")
@PathVariable("courseId") Long courseId,
HttpServletRequest request) {
log.info("Deleting chapter {} from course {}", chapterId, courseId);
Long creatorId =RequestAttributeExtractor.extractCreatorId(request);
// ✅ KHÔNG cần try-catch - để GlobalExceptionHandler xử lý
chapterService.deleteChapter(chapterId, courseId, creatorId);
return ResponseEntity.noContent().build();
}
Response:
Unit Test Suite: ModuleController.deleteChapter()
Test Case Matrix:
Category
Test Case
Input
Expected Result
HAPPY PATH
Delete chapter successfully
chapterId=1, courseId=1, creatorId=100
HttpStatus.NO_CONTENT (204), Empty body, Service called once
HAPPY PATH
Delete middle chapter
chapterId=3, courseId=1, creatorId=100
HttpStatus.NO_CONTENT (204), Remaining chapters reordered
HAPPY PATH
Delete last chapter
chapterId=5 (last), courseId=1, creatorId=100
HttpStatus.NO_CONTENT (204), Chapter deleted successfully
VALIDATION EDGE CASES
Zero chapter ID
chapterId=0, courseId=1, creatorId=100
Throws validation error: "Chapter ID must be positive"
VALIDATION EDGE CASES
Negative chapter ID
chapterId=-1, courseId=1, creatorId=100
Throws validation error: "Chapter ID must be positive"
VALIDATION EDGE CASES
Zero course ID
chapterId=1, courseId=0, creatorId=100
Throws validation error: "Course ID must be positive"
VALIDATION EDGE CASES
Negative course ID
chapterId=1, courseId=-1, creatorId=100
Throws validation error: "Course ID must be positive"
ERROR SCENARIOS
Chapter not found
chapterId=999, courseId=1, creatorId=100
Throws ResourceNotFoundException: "Chapter not found with ID: 999"
ERROR SCENARIOS
Course not found
chapterId=1, courseId=999, creatorId=100
Throws ResourceNotFoundException: "Course not found with ID: 999"
ERROR SCENARIOS
Chapter doesn't belong to course
chapterId=1 (from course 2), courseId=1, creatorId=100
Throws InvalidOperationException: "Chapter does not belong to course"
ERROR SCENARIOS
Creator not authorized
chapterId=1, courseId=1, creatorId=999 (not owner)
Throws UnauthorizedException: "Not authorized to delete chapter"
ERROR SCENARIOS
Creator ID not found in request
chapterId=1, courseId=1, request (no creator attribute)
Throws UnauthorizedException: "Creator ID not found in request"
ERROR SCENARIOS
Null HTTP request
chapterId=1, courseId=1, request=null
Throws IllegalArgumentException: "Request cannot be null"
INTEGRATION & STATE
Verify service called correctly
chapterId=1, courseId=1, creatorId=100
chapterService.deleteChapter(1L, 1L, 100L) called once
INTEGRATION & STATE
Verify service NOT called on validation fail
chapterId=0, courseId=1, creatorId=100
chapterService.deleteChapter() never called
INTEGRATION & STATE
Course chapter count decreases
Before: 5 chapters, Delete one
Course now has 4 chapters, order reindexed
Prompt 6: Generate Test Cases for updateChapter()
Comprehensive Unit Test Cases for updateChapter()
Generate a full set of high-quality unit test for the updateChapter()operation in the Course Management domain. Focus on both Happy path scenarios and critical edge cases, ensuring Error scenarios, Integration with cart state
@PutMapping("/{chapterID}/update")
public ResponseEntity<?>updateChapter(@RequestParam String name ,HttpServletRequest request,
@PathVariable("chapterID")long chapterId){
System.out.print("d");
Long creatorId =RequestAttributeExtractor.extractCreatorId(request);
this.chapterService.updateChapter(creatorId, name, chapterId);
return ResponseEntity.noContent().build();
}
Response:
Unit Test Suite: ModuleController.updateChapter()
Test Case Matrix:
Category
Test Case
Input
Expected Result
HAPPY PATH
Update chapter name successfully
name="Advanced Java", chapterId=1, creatorId=100
HttpStatus.NO_CONTENT (204), Empty body, Service called once
HAPPY PATH
Update with special characters
name="Chapter #2: C++ & OOP", chapterId=1, creatorId=100
HttpStatus.NO_CONTENT (204), Special chars preserved
HAPPY PATH
Update with minimum name length
name="A", chapterId=1, creatorId=100
HttpStatus.NO_CONTENT (204), Single character accepted
VALIDATION EDGE CASES
Blank chapter name
name="", chapterId=1, creatorId=100
Throws validation error: "Chapter name is required"
VALIDATION EDGE CASES
Null chapter name
name=null, chapterId=1, creatorId=100
Throws validation error: "Chapter name is required"
VALIDATION EDGE CASES
Whitespace-only name
name=" ", chapterId=1, creatorId=100
Throws validation error: "Chapter name is required"
VALIDATION EDGE CASES
Zero chapter ID
name="Test", chapterId=0, creatorId=100
Throws validation error or handled by service layer
VALIDATION EDGE CASES
Negative chapter ID
name="Test", chapterId=-1, creatorId=100
Throws validation error or handled by service layer
ERROR SCENARIOS
Chapter not found
name="Test", chapterId=999, creatorId=100
Throws ResourceNotFoundException: "Chapter not found with ID: 999"
ERROR SCENARIOS
Creator not authorized
name="Test", chapterId=1, creatorId=999 (not owner)
Throws UnauthorizedException: "Not authorized to update chapter"
ERROR SCENARIOS
Duplicate chapter name in course
name="Introduction" (exists), chapterId=2, creatorId=100
Throws DuplicateResourceException: "Chapter name already exists in course"
ERROR SCENARIOS
Creator ID not found in request
name="Test", chapterId=1, request (no creator attribute)
Throws UnauthorizedException: "Creator ID not found in request"
ERROR SCENARIOS
Null HTTP request
name="Test", chapterId=1, request=null
Throws IllegalArgumentException: "Request cannot be null"
ERROR SCENARIOS
Service returns/throws exception
name="Test", chapterId=1, Service throws error
Exception propagated to GlobalExceptionHandler
INTEGRATION
Verify service called correctly
name="Updated", chapterId=1, creatorId=100
chapterService.updateChapter(100L, "Updated", 1L) called once
INTEGRATION
Verify service NOT called on validation fail
name="", chapterId=1, creatorId=100
chapterService.updateChapter() never called