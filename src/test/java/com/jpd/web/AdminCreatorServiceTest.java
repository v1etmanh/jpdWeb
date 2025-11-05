package com.jpd.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.jpd.web.dto.AdminCreatorDetailDto;
import com.jpd.web.dto.AdminCreatorListDto;
import com.jpd.web.dto.CertificateApprovalDto;
import com.jpd.web.helper.TestDataReader;
import com.jpd.web.model.*;
import com.jpd.web.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.AdminCreatorService;
import com.jpd.web.service.AuditLogService;
import com.jpd.web.service.utils.ValidationResources;
import org.threeten.bp.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Creator Service Tests")
public class AdminCreatorServiceTest {

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private ValidationResources validationResources;

    @Mock
    private AuditLogService auditLogService;

    // @Mock
    // private WarningRepository warningRepository;  // Chưa có trong project

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AdminCreatorService adminCreatorService;

    @Nested
    @DisplayName("Approve Certificate - BR-19")
    class ApproveCertificateTests {

        @Nested
        @DisplayName("Data-Driven Tests")
        class DataDrivenTests {

            @TestFactory
            @DisplayName("Run all approve certificate test cases from CSV")
            List<DynamicTest> testApproveCertificateFromCSV() throws IOException {
                List<Map<String, String>> testData = TestDataReader
                        .readTestDataFromCSV("approve-certificate-tests.csv");

                List<DynamicTest> tests = new ArrayList<>();

                for (Map<String, String> data : testData) {
                    String testCase = data.get("testCase");

                    DynamicTest test = DynamicTest.dynamicTest(testCase, () -> {
                        executeTestCase(data);
                    });

                    tests.add(test);
                }

                return tests;
            }

            private void executeTestCase(Map<String, String> data) {
                // 1) Reset mocks để mỗi test case độc lập
                reset(creatorRepository, validationResources, auditLogService);

                // 2) Parse test data từ CSV
                final Long creatorId = TestDataReader.parseLong(data.get("creatorId"));
                final Status creatorStatus = TestDataReader.parseEnum(Status.class, data.get("creatorStatus"));
                final String adminEmail = data.get("adminEmail");
                final String adminNote = (data.get("adminNote") == null || data.get("adminNote").equalsIgnoreCase("null"))
                        ? null
                        : data.get("adminNote");
                final boolean shouldSucceed = TestDataReader.parseBoolean(data.get("shouldSucceed"));
                final String expectedNewStatusStr = data.get("expectedNewStatus");
                final String expectedErrorMessage = data.get("expectedErrorMessage");
                final boolean shouldLogAudit = TestDataReader.parseBoolean(data.get("shouldLogAudit"));
                final boolean shouldSaveCreator = TestDataReader.parseBoolean(data.get("shouldSaveCreator"));

                // Convert expected status nếu có
                final Status expectedNewStatus = (expectedNewStatusStr == null || expectedNewStatusStr.isBlank())
                        ? null
                        : Status.valueOf(expectedNewStatusStr);

                // 3) Setup mocks theo test case
                if (creatorId != null && creatorId == 999L) {
                    // Creator not found
                    when(validationResources.validateCreatorExists(eq(creatorId)))
                            .thenThrow(new NoSuchElementException("Creator not found"));
                } else {
                    Creator mock = new Creator();
                    mock.setCreatorId(creatorId);
                    mock.setStatus(creatorStatus);
                    when(validationResources.validateCreatorExists(eq(creatorId))).thenReturn(mock);

                    // save trả lại entity truyền vào để kiểm tra trạng thái sau khi service set
                    when(creatorRepository.save(any(Creator.class)))
                            .thenAnswer(inv -> inv.getArgument(0));
                }

                // 4) Execute service method + 5) Verify results
                if (shouldSucceed) {
                    // Không được ném exception
                    assertDoesNotThrow(() ->
                            adminCreatorService.approveCertificate(creatorId, adminEmail, adminNote)
                    );

                    // Khi success, validateCreatorExists chắc chắn phải thành công → lấy instance đã mock để assert
                    ArgumentCaptor<Creator> savedCaptor = ArgumentCaptor.forClass(Creator.class);
                    if (shouldSaveCreator) {
                        verify(creatorRepository, times(1)).save(savedCaptor.capture());
                    }

                    // Lấy Creator đã validate để assert status
                    Creator assertedCreator;
                    if (shouldSaveCreator) {
                        assertedCreator = savedCaptor.getValue();
                    } else {
                        // Nếu không save (theo CSV), vẫn lấy instance từ validate mock
                        assertedCreator = validationResources.validateCreatorExists(creatorId);
                    }

                    assertAll("Success assertions",
                            () -> {
                                if (expectedNewStatus != null) {
                                    assertEquals(expectedNewStatus, assertedCreator.getStatus(),
                                            "Creator status must be updated to expectedNewStatus");
                                }
                            },
                            () -> {
                                if (shouldLogAudit) {
                                    // adminNote == null → service log "N/A" theo code
                                    String expectedNotePart = (adminNote == null) ? "N/A" : adminNote;
                                    verify(auditLogService, times(1)).logAction(
                                            eq("APPROVE_CERT"),
                                            eq(creatorId),
                                            eq(adminEmail),
                                            argThat(msg -> msg != null && msg.contains(expectedNotePart))
                                    );
                                } else {
                                    verify(auditLogService, never()).logAction(any(), any(), any(), any());
                                }
                            }
                    );

                    // Không mong muốn interaction dư thừa
                    verify(validationResources, times(1)).validateCreatorExists(eq(creatorId));

                } else {
                    // Trường hợp thất bại: phải ném exception
                    Exception ex = assertThrows(Exception.class, () ->
                            adminCreatorService.approveCertificate(creatorId, adminEmail, adminNote)
                    );

                    assertAll("Failure assertions",
                            () -> {
                                if (expectedErrorMessage != null && !expectedErrorMessage.isBlank()) {
                                    assertTrue(ex.getMessage() != null &&
                                                    ex.getMessage().toLowerCase().contains(expectedErrorMessage.toLowerCase()),
                                            () -> "Expected error message to contain: " + expectedErrorMessage +
                                                    " but got: " + ex.getMessage());
                                }
                            },
                            () -> {
                                // Không được save / không được log audit khi fail
                                verify(creatorRepository, never()).save(any());
                                verify(auditLogService, never()).logAction(any(), any(), any(), any());
                            }
                    );

                    // Với creator_not_found: validateCreatorExists được gọi 1 lần và ném lỗi
                    if (creatorId != null) {
                        verify(validationResources, times(1)).validateCreatorExists(eq(creatorId));
                    }
                }

                // 6) No unnecessary mock interactions
                verifyNoMoreInteractions(auditLogService);
            }
        }
    }
    @Nested
    @DisplayName("GET Creator List")
    class GetCreatorListTests {

        // Helpers tạo dữ liệu giả cho danh sách creators
        private List<Creator> makeCreators(int total, Status statusForAll, boolean withNullName) {
            List<Creator> list = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                Creator c = new Creator();
                c.setCreatorId(100L + i);
                if (withNullName && i == 0) {
                    c.setFullName(null);
                } else {
                    // Tạo tên đa dạng để search: John, Nguyen, Tran
                    String[] pool = {"John Doe", "Nguyen Van A", "Tran Thi B", "Alice", "Bob"};
                    c.setFullName(pool[i % pool.length]);
                }
                c.setStatus(statusForAll != null ? statusForAll
                        : (i % 5 == 0 ? Status.PENDING : (i % 4 == 0 ? Status.SUSPENDED : Status.SUCCESS)));

                // Set các field cần thiết để tránh NullPointerException
                c.setCourses(new ArrayList<>());
                c.setWarningCount(0);
                c.setBalance(0.0);
                c.setCreateDate(java.sql.Date.valueOf(LocalDate.now()));

                list.add(c);
            }
            return list;
        }

        @Nested
        @DisplayName("Data-Driven Tests from CSV")
        class DataDrivenTests {

            @TestFactory
            @DisplayName("Run all admin-creator-list-tests.csv")
            List<DynamicTest> testFromCSV() throws IOException {
                List<Map<String, String>> rows = TestDataReader.readTestDataFromCSV("admin-creator-list-tests.csv");
                List<DynamicTest> tests = new ArrayList<>();
                for (Map<String, String> row : rows) {
                    String name = row.get("testCase");
                    tests.add(DynamicTest.dynamicTest(name, () -> execute(row)));
                }
                return tests;
            }

            private void execute(Map<String, String> r) {
                reset(creatorRepository);

                Status filterStatus = TestDataReader.parseEnum(Status.class, r.get("status"));
                String search = r.get("searchKeyword");
                int page = Optional.ofNullable(TestDataReader.parseInteger(r.get("page"))).orElse(0);
                int size = Optional.ofNullable(TestDataReader.parseInteger(r.get("size"))).orElse(10);
                int mockCount = Optional.ofNullable(TestDataReader.parseInteger(r.get("mockCreatorCount"))).orElse(0);
                int expectedCount = Optional.ofNullable(TestDataReader.parseInteger(r.get("expectedResultCount"))).orElse(0);
                boolean shouldSucceed = TestDataReader.parseBoolean(r.get("shouldSucceed"));
                String notes = r.get("notes");

                // Chuẩn bị dữ liệu giả
                boolean withNullName = "null_fullname_handling".equals(r.get("testCase"));
                List<Creator> all = makeCreators(mockCount, null, withNullName);

                // Mock repository theo logic của service
                if (filterStatus != null) {
                    // Service gọi findAllByStatus(status) khi có filter
                    List<Creator> filteredByStatus = all.stream()
                            .filter(c -> c.getStatus() == filterStatus)
                            .collect(Collectors.toList());
                    when(creatorRepository.findAllByStatus(filterStatus)).thenReturn(filteredByStatus);
                } else {
                    // Service gọi findAll() khi không có status filter
                    when(creatorRepository.findAll()).thenReturn(all);
                }

                // Mock các method phụ cho DTO mapping
                for (Creator c : all) {
                    when(creatorRepository.countTotalStudentsByCreatorId(c.getCreatorId())).thenReturn(0);
                    when(creatorRepository.getAverageRatingByCreatorId(c.getCreatorId())).thenReturn(0.0);
                }

                // Tự tính expectedFiltered theo đúng rule của service
                String q = (search == null ? "" : search.toLowerCase());
                Status s = filterStatus;
                List<Creator> expectedFiltered = all.stream()
                        .filter(c -> s == null || c.getStatus() == s)
                        .filter(c -> {
                            if (q.isBlank()) return true;
                            String name = Optional.ofNullable(c.getFullName()).orElse("");
                            return name.toLowerCase().contains(q);
                        })
                        .toList();

                int from = Math.min(page * size, expectedFiltered.size());
                int to   = Math.min(from + size, expectedFiltered.size());
                int expectedPageCount = to - from;

                Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(filterStatus, search, page, size);

                assertAll("List assertions: " + r.get("testCase"),
                        () -> assertTrue(shouldSucceed, "Should succeed"),
                        () -> assertEquals(expectedPageCount, result.getContent().size(), "Page content size mismatch"),
                        () -> assertEquals(expectedFiltered.size(), result.getTotalElements(), "TotalElements after filters mismatch")
                );
            }
        }
    }
    @Nested
    @DisplayName("Ban Creator")
    class BanCreatorTests {

        private Creator mkCreatorWithCourses(Long id, int courseCount) {
            Creator c = new Creator();
            c.setCreatorId(id);
            c.setStatus(Status.SUCCESS);
            List<Course> courses = new ArrayList<>();
            for (int i = 0; i < courseCount; i++) {
                Course course = new Course();
                course.setCourseId(1000L + i);
                course.setBan(false);
                courses.add(course);
            }
            c.setCourses(courses);
            return c;
        }

        @TestFactory
        @DisplayName("Run ban-creator-tests.csv")
        List<DynamicTest> testFromCSV() throws IOException {
            List<Map<String, String>> rows = TestDataReader.readTestDataFromCSV("ban-creator-tests.csv");
            List<DynamicTest> tests = new ArrayList<>();
            for (Map<String, String> r : rows) {
                tests.add(DynamicTest.dynamicTest(r.get("testCase"), () -> execute(r)));
            }
            return tests;
        }

        private void execute(Map<String, String> r) {
            reset(validationResources, creatorRepository, auditLogService);

            Long creatorId = TestDataReader.parseLong(r.get("creatorId"));
            Integer durationDays = "null".equalsIgnoreCase(r.get("durationDays")) ? null
                    : TestDataReader.parseInteger(r.get("durationDays"));
            String reason = r.get("reason");
            String adminEmail = r.get("adminEmail");
            boolean hasCourses = TestDataReader.parseBoolean(r.get("hasCourses"));
            int courseCount = Optional.ofNullable(TestDataReader.parseInteger(r.get("courseCount"))).orElse(0);
            boolean shouldSucceed = TestDataReader.parseBoolean(r.get("shouldSucceed"));
            boolean shouldBanCourses = TestDataReader.parseBoolean(r.get("shouldBanCourses"));
            String expectedError = r.get("expectedError");
            Status expectedStatus = Status.BANNED;
            String expectedBannedUntil = r.get("expectedBannedUntil"); // NULL | NOT_NULL

            if (Objects.equals(creatorId, 999L)) {
                when(validationResources.validateCreatorExists(creatorId))
                        .thenThrow(new NoSuchElementException("Creator not found"));
            } else {
                Creator c = hasCourses ? mkCreatorWithCourses(creatorId, courseCount) : mkCreatorWithCourses(creatorId, 0);
                when(validationResources.validateCreatorExists(creatorId)).thenReturn(c);
                when(creatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            }

            if (shouldSucceed) {
                assertDoesNotThrow(() -> adminCreatorService.banCreator(creatorId, reason, durationDays, adminEmail));
                Creator asserted = validationResources.validateCreatorExists(creatorId);

                assertAll("Ban assertions",
                        () -> assertTrue(asserted.isBan()),
                        () -> assertEquals(expectedStatus, asserted.getStatus()),
                        () -> {
                            if ("NULL".equalsIgnoreCase(expectedBannedUntil)) {
                                assertNull(asserted.getBannedUntil());
                            } else if ("NOT_NULL".equalsIgnoreCase(expectedBannedUntil)) {
                                assertNotNull(asserted.getBannedUntil());
                            }
                        },
                        () -> {
                            if (shouldBanCourses && asserted.getCourses() != null) {
                                assertTrue(asserted.getCourses().stream().allMatch(Course::isBan));
                            } else if (!shouldBanCourses) {
                                // Không có courses hoặc không yêu cầu ban courses
                                if (asserted.getCourses() != null) {
                                    assertTrue(asserted.getCourses().isEmpty());
                                }
                            }
                        },
                        () -> verify(auditLogService, times(1))
                                .logAction(eq("BAN_CREATOR"), eq(creatorId), eq(adminEmail), anyString())
                );
            } else {
                Exception ex = assertThrows(Exception.class,
                        () -> adminCreatorService.banCreator(creatorId, reason, durationDays, adminEmail));
                if (expectedError != null && !expectedError.isBlank()) {
                    assertTrue(ex.getMessage().toLowerCase().contains(expectedError.toLowerCase()));
                }
                verify(creatorRepository, never()).save(any());
                verify(auditLogService, never()).logAction(any(), any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("Unban Creator")
    class UnbanCreatorTests {

        @Test
        void shouldUnbanAndCascade() {
            Creator c = new Creator();
            c.setCreatorId(20L);
            c.setStatus(Status.BANNED);
            c.setBan(true);
            c.setBannedUntil(java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(5)));
            Course course = new Course();
            course.setBan(true);
            c.setCourses(new ArrayList<>(List.of(course)));

            when(validationResources.validateCreatorExists(20L)).thenReturn(c);
            when(creatorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            adminCreatorService.unbanCreator(20L, "ok", "admin@x");

            assertAll(
                    () -> assertFalse(c.isBan()),
                    () -> assertEquals(Status.SUCCESS, c.getStatus()),
                    () -> assertNull(c.getBannedUntil()),
                    () -> assertFalse(c.getCourses().get(0).isBan()),
                    () -> verify(auditLogService).logAction(eq("UNBAN_CREATOR"), eq(20L), eq("admin@x"), eq("ok"))
            );
        }
    }

    @Nested
    @DisplayName("GET Pending Certificates")
    class GetPendingCertificatesTests {

        @Test
        void shouldReturnPendingCertificates() {
            Creator p1 = new Creator();
            p1.setCreatorId(1L);
            p1.setFullName("A");
            p1.setStatus(Status.PENDING);

            Creator p2 = new Creator();
            p2.setCreatorId(2L);
            p2.setFullName("B");
            p2.setStatus(Status.PENDING);

            when(creatorRepository.findAllByStatus(Status.PENDING)).thenReturn(List.of(p1, p2));

            List<CertificateApprovalDto> list = adminCreatorService.getPendingCertificates();

            assertAll(
                    () -> assertEquals(2, list.size()),
                    () -> assertEquals(Status.PENDING, list.get(0).getStatus())
            );
        }
    }

    @Nested
    @DisplayName("GET Creator Detail")
    class GetCreatorDetailTests {

        @Test
        void mapsRecentReportsAndCoursesLimits() {
            Creator creator = new Creator();
            creator.setCreatorId(30L);
            creator.setFullName("Tester");
            creator.setStatus(Status.SUCCESS);

            // 6 courses → expect limit 5 in DTO
            List<Course> courses = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                Course c = new Course();
                c.setCourseId(100L + i);
                c.setPrice(10.0);
                c.setCreatedAt(java.time.LocalDate.now());
                c.setEnrollments(new ArrayList<>());
                courses.add(c);
            }
            creator.setCourses(courses);

            // 12 reports → expect limit 10
            List<Report> reports = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                Report rp = new Report();
                rp.setReportId(500L + i);
                // rp.setType(ReportType.ABUSE);
                rp.setDetail("d" + i);
                //rp.setStatus(ReportStatus.OPEN);
                rp.setCreatedAt(java.time.LocalDateTime.now());
                reports.add(rp);
            }

            when(validationResources.validateCreatorExists(30L)).thenReturn(creator);
            when(creatorRepository.countTotalStudentsByCreatorId(30L)).thenReturn(7);
            when(creatorRepository.getAverageRatingByCreatorId(30L)).thenReturn(4.2);
            when(reportRepository.findByCreator_CreatorId(30L)).thenReturn(reports);

            AdminCreatorDetailDto dto = adminCreatorService.getCreatorDetail(30L);

            assertAll(
                    () -> assertEquals(5, dto.getRecentCourses().size()),
                    () -> assertEquals(10, dto.getRecentReports().size()),
                    () -> assertEquals(7, dto.getTotalStudents()),
                    () -> assertEquals(4.2, dto.getAvgRating())
            );
        }
    }
}