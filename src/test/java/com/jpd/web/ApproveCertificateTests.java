package com.jpd.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

import com.jpd.web.helper.TestDataReader;
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

import com.jpd.web.model.Creator;
import com.jpd.web.model.Status;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.service.AdminCreatorService;
import com.jpd.web.service.AuditLogService;
import com.jpd.web.service.utils.ValidationResources;

@ExtendWith(MockitoExtension.class)
@Nested
@DisplayName("Approve Certificate - BR-19")
public class ApproveCertificateTests {

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private ValidationResources validationResources;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminCreatorService adminCreatorService;

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