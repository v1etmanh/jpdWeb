package com.jpd.web;

import com.jpd.web.helper.TestDataReader;
import com.jpd.web.model.AuditLog;
import com.jpd.web.repository.AuditLogRepository;
import com.jpd.web.service.AuditLogService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    // ---------------------------- LOG ACTION ----------------------------
    @Nested
    @DisplayName("logAction Tests")
    class LogActionTests {

        @TestFactory
        @DisplayName("CSV-driven: logAction")
        Collection<DynamicTest> testLogActionFromCsv() throws Exception {
            List<Map<String, String>> rows = TestDataReader.readCsvFlexible("log_action_test_cases.csv");

            if (rows == null || rows.isEmpty()) {
                return Collections.singletonList(
                        DynamicTest.dynamicTest("CSV file missing", () ->
                                fail("CSV file not found or empty"))
                );
            }

            return rows.stream().map(row -> {
                // Lấy test name
                String caseId = nv(row.get("caseId"));
                String title = nv(row.get("title"));
                String testName = title.isBlank() ? caseId : caseId + ": " + title;

                return DynamicTest.dynamicTest(testName, () -> {
                    // Đọc data từ CSV
                    String actionType       = nv(row.get("actionType"));
                    String targetCreatorStr = nv(row.get("targetCreatorId"));
                    String adminEmail       = nv(row.get("adminEmail"));
                    String reason           = nv(row.get("reason"));
                    String expectException  = nv(row.get("expectException"));

                    // Parse targetCreatorId
                    Long targetCreatorId = parseNullableLong(targetCreatorStr);

                    // DEBUG: In ra để kiểm tra
                    System.out.println("Test: " + testName);
                    System.out.println("  actionType=" + actionType);
                    System.out.println("  targetCreatorId=" + targetCreatorId);
                    System.out.println("  adminEmail=" + adminEmail);
                    System.out.println("  expectException=" + expectException);

                    // Xác định test case type
                    boolean expectSuccess = expectException.isBlank();

                    // Xác định liệu có validation error không (dựa vào service logic)
                    boolean hasValidationError =
                            actionType.isBlank() ||
                                    targetCreatorId == null ||
                                    adminEmail.isBlank();

                    if (expectException.equals("DataIntegrityViolationException")) {
                        // ═══ DATABASE EXCEPTION CASE ═══
                        when(auditLogRepository.save(any(AuditLog.class)))
                                .thenThrow(new DataIntegrityViolationException("DB error"));

                        assertThrows(DataIntegrityViolationException.class, () ->
                                auditLogService.logAction(actionType, targetCreatorId, adminEmail, reason));

                    } else if (expectSuccess && !hasValidationError) {
                        // ═══ SUCCESS CASE ═══
                        when(auditLogRepository.save(any(AuditLog.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                        assertDoesNotThrow(() ->
                                auditLogService.logAction(actionType, targetCreatorId, adminEmail, reason));

                        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
                        verify(auditLogRepository, times(1)).save(cap.capture());

                        AuditLog saved = cap.getValue();
                        assertEquals(actionType, saved.getActionType());
                        assertEquals(targetCreatorId, saved.getTargetCreatorId());
                        assertEquals(adminEmail, saved.getAdminEmail());
                        assertEquals(reason, saved.getReason());

                    } else {
                        // ═══ VALIDATION ERROR CASE ═══
                        assertThrows(IllegalArgumentException.class, () ->
                                auditLogService.logAction(actionType, targetCreatorId, adminEmail, reason));

                        verify(auditLogRepository, never()).save(any());
                    }

                    clearInvocations(auditLogRepository);
                });
            }).collect(Collectors.toList());
        }

        @Test
        @DisplayName("should save with all fields correctly")
        void shouldSaveAuditLogWithAllFields() {
            when(auditLogRepository.save(any(AuditLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            auditLogService.logAction("TEST_ACTION", 10L, "admin@test.com", "test note");

            ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(cap.capture());

            AuditLog saved = cap.getValue();
            assertEquals("TEST_ACTION", saved.getActionType());
            assertEquals(10L, saved.getTargetCreatorId());
            assertEquals("admin@test.com", saved.getAdminEmail());
            assertEquals("test note", saved.getReason());
        }

        @Test
        @DisplayName("should reject null action type")
        void shouldRejectNullActionType() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    auditLogService.logAction(null, 1L, "admin@test.com", "note"));

            assertEquals("Action cannot be null", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject blank action type")
        void shouldRejectBlankActionType() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    auditLogService.logAction("   ", 1L, "admin@test.com", "note"));

            assertEquals("Action cannot be null", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject null target creator ID")
        void shouldRejectNullTargetCreatorId() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    auditLogService.logAction("ACTION", null, "admin@test.com", "note"));

            assertEquals("Entity ID cannot be null", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject null admin email")
        void shouldRejectNullAdminEmail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    auditLogService.logAction("ACTION", 1L, null, "note"));

            assertEquals("Performed by cannot be empty", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject blank admin email")
        void shouldRejectBlankAdminEmail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    auditLogService.logAction("ACTION", 1L, "   ", "note"));

            assertEquals("Performed by cannot be empty", ex.getMessage());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("should accept null or empty reason")
        void shouldAcceptNullOrEmptyReason() {
            when(auditLogRepository.save(any(AuditLog.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() ->
                    auditLogService.logAction("ACTION", 1L, "admin@test.com", null));

            assertDoesNotThrow(() ->
                    auditLogService.logAction("ACTION", 2L, "admin@test.com", ""));

            verify(auditLogRepository, times(2)).save(any());
        }
    }

    // ------------------------ GET LOGS BY CREATOR ------------------------
    @Nested
    @DisplayName("getLogsByCreator Tests")
    class GetLogsByCreatorTests {

        @Test
        @DisplayName("should return logs for creator when logs exist")
        void shouldReturnLogsForCreator_WhenLogsExist() {
            Long creatorId = 1L;
            List<AuditLog> fake = Arrays.asList(
                    build("CREATOR_APPROVED", creatorId, "a@a.com", "d1"),
                    build("CREATOR_BANNED", creatorId, "a@a.com", "d2"),
                    build("CREATOR_UNBANNED", creatorId, "a@a.com", "d3")
            );
            when(auditLogRepository.findByTargetCreatorIdOrderByTimestampDesc(creatorId))
                    .thenReturn(fake);

            List<AuditLog> result = auditLogService.getLogsByCreator(creatorId);

            assertEquals(3, result.size());
            assertEquals("CREATOR_APPROVED", result.get(0).getActionType());
            verify(auditLogRepository).findByTargetCreatorIdOrderByTimestampDesc(creatorId);
        }

        @Test
        @DisplayName("should return empty list when creator has no logs")
        void shouldReturnEmpty_WhenNoLogsForCreator() {
            Long creatorId = 999L;
            when(auditLogRepository.findByTargetCreatorIdOrderByTimestampDesc(creatorId))
                    .thenReturn(Collections.emptyList());

            List<AuditLog> result = auditLogService.getLogsByCreator(creatorId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(auditLogRepository).findByTargetCreatorIdOrderByTimestampDesc(creatorId);
        }
    }

    // -------------------------- GET LOGS BY ADMIN -------------------------
    @Nested
    @DisplayName("getLogsByAdmin Tests")
    class GetLogsByAdminTests {

        @Test
        @DisplayName("should return logs for admin when logs exist")
        void shouldReturnLogsForAdmin_WhenLogsExist() {
            String admin = "admin@test.com";
            List<AuditLog> fake = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                fake.add(build("ACTION_" + i, 100L + i, admin, "d" + i));
            }
            when(auditLogRepository.findByAdminEmailOrderByTimestampDesc(admin))
                    .thenReturn(fake);

            List<AuditLog> result = auditLogService.getLogsByAdmin(admin);

            assertEquals(5, result.size());
            assertEquals(admin, result.get(0).getAdminEmail());
            verify(auditLogRepository).findByAdminEmailOrderByTimestampDesc(admin);
        }

        @Test
        @DisplayName("should return empty list when admin has no logs")
        void shouldReturnEmpty_WhenNoLogsForAdmin() {
            String admin = "unknown@test.com";
            when(auditLogRepository.findByAdminEmailOrderByTimestampDesc(admin))
                    .thenReturn(Collections.emptyList());

            List<AuditLog> result = auditLogService.getLogsByAdmin(admin);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(auditLogRepository).findByAdminEmailOrderByTimestampDesc(admin);
        }
    }

    // ------------------------------- HELPERS ------------------------------
    private static String nv(String s) {
        return s == null ? "" : s.trim();
    }

    private static Long parseNullableLong(String s) {
        if (s == null || s.isBlank() || s.trim().equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static AuditLog build(String action, Long creatorId, String admin, String reason) {
        AuditLog a = new AuditLog();
        a.setActionType(action);
        a.setTargetCreatorId(creatorId);
        a.setAdminEmail(admin);
        a.setReason(reason);

        try {
            AuditLog.class.getMethod("setTimestamp", Instant.class).invoke(a, Instant.now());
        } catch (Exception ignored) {
        }
        return a;
    }
}