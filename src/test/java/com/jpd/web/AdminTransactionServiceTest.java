package com.jpd.web;


import com.jpd.web.dto.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerTransactionRepository;
import com.jpd.web.repository.projection.CourseRevenueProjection;
import com.jpd.web.repository.projection.CreatorRevenueProjection;
import com.jpd.web.service.AdminTransactionService;
import com.jpd.web.service.AuditLogService;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.helper.TransactionTestDataBuilder;
import com.jpd.web.helper.TestDataReader; // <- dùng helper custom của bạn

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTransactionService Tests (TestDataReader + @TestFactory)")
class AdminTransactionServiceTest {

    @Mock CustomerTransactionRepository transactionRepository;
    @Mock CreatorRepository creatorRepository;
    @Mock ValidationResources validationResources;
    @Mock AuditLogService auditLogService;

    @InjectMocks
    AdminTransactionService service;

    // ======= Helpers for projections =======
    private CourseRevenueProjection mkCourseProj(long courseId, String name, String img, double revenue, long enroll) {
        return new CourseRevenueProjection() {
            public Long getCourseId() { return courseId; }
            public String getCourseName() { return name; }
            public String getImageUrl() { return img; }
            public Double getTotalRevenue() { return revenue; }
            public Long getEnrollmentCount() { return enroll; }
        };
    }
    private CreatorRevenueProjection mkCreatorProj(long id, String name, double revenue, long courses) {
        return new CreatorRevenueProjection() {
            public Long getCreatorId() { return id; }
            public String getCreatorName() { return name; }
            public Double getTotalRevenue() { return revenue; }
            public Long getCourseCount() { return courses; }
        };
    }

    // ======= getTransactionList() =======
    @Nested
    @DisplayName("getTransactionList")
    class GetTransactionList {

        @TestFactory
        @DisplayName("Data-driven via txn_list_filters.csv")
        Stream<DynamicTest> list_with_filters() throws Exception {
                return TestDataReader.read("/testdata/txn_list_filters.csv").stream().map(row -> {
                String caseId = row.get("caseId");
                String sortBy = row.get("sortBy");
                String sortDir = row.get("sortDirection");
                int page = Integer.parseInt(row.get("page"));
                int size = Integer.parseInt(row.get("size"));
                int expectCount = Integer.parseInt(row.get("expectCount"));
                boolean expectSortDirValid = Boolean.parseBoolean(row.get("expectSortDirValid"));

                return DynamicTest.dynamicTest(
                        "Case " + caseId + " sortBy=" + sortBy + " dir=" + sortDir,
                        () -> {
                            Mockito.reset(transactionRepository);
                            TransactionFilterDto f = new TransactionFilterDto();
                            f.setSortBy(sortBy);
                            f.setSortDirection(sortDir);
                            f.setPage(page);
                            f.setSize(size);

                            // Mock page content
                            Creator cr = TransactionTestDataBuilder.mkCreator(10L, "Creator A", false, Status.SUCCESS);
                            Customer cu = TransactionTestDataBuilder.mkCustomer(1L, "Alice", "Nguyen", "a@ex.com");
                            Course co = TransactionTestDataBuilder.mkCourse(100L, "Course A", 1_500_000, "img", cr);
                            Enrollment en = TransactionTestDataBuilder.mkEnrollment(500L, cu, co, true);

                            List<CustomerTransaction> txs = new ArrayList<>();
                            for (int i = 0; i < expectCount; i++) {
                                txs.add(TransactionTestDataBuilder.mkTx(9000L + i, en, 1_000_000, 200_000, 800_000, "SUCCESS"));
                            }
                            lenient().when(transactionRepository.findAll(
                                    any(Specification.class),
                                    any(Pageable.class)))
                                    .thenReturn(new PageImpl<>(txs, PageRequest.of(page, size), expectCount));

                            if (!expectSortDirValid) {
                                assertThrows(IllegalArgumentException.class, () -> service.getTransactionList(f));
                                return;
                            }

                            Page<TransactionListDto> result = service.getTransactionList(f);

                            assertEquals(expectCount, result.getContent().size());
                            assertEquals(page, result.getNumber());
                            assertEquals(size, result.getSize());
                            verify(transactionRepository).findAll(any(Specification.class), any(Pageable.class));
                        });
            });
        }
    }

    // ======= getTransactionDetail() =======
    @Nested
    @DisplayName("getTransactionDetail")
    class GetTransactionDetail {

        @Test
        void should_map_full_detail_and_percentages() {
            Creator cr = TransactionTestDataBuilder.mkCreator(10L, "Creator A", false, Status.SUCCESS);
            Customer cu = TransactionTestDataBuilder.mkCustomer(1L, "Alice", "Nguyen", "a@ex.com");
            Course co = TransactionTestDataBuilder.mkCourse(100L, "Course A", 2_000_000, "img", cr);
            Enrollment en = TransactionTestDataBuilder.mkEnrollment(500L, cu, co, true);
            CustomerTransaction tx = TransactionTestDataBuilder.mkTx(9001L, en, 2_000_000, 400_000, 1_600_000, "SUCCESS");

            when(transactionRepository.findById(9001L)).thenReturn(Optional.of(tx));

            TransactionDetailDto dto = service.getTransactionDetail(9001L);

            assertEquals(9001L, dto.getTransactionId());
            assertEquals(20.0, dto.getAdminPercentage(), 0.0001);
            assertEquals(80.0, dto.getCreatorPercentage(), 0.0001);
            assertEquals("Alice Nguyen", dto.getCustomerInfo().getName());
            assertEquals(100L, dto.getCourseInfo().getCourseId());
        }

        @Test
        void should_return_0_percent_when_amount_zero() {
            Creator cr = TransactionTestDataBuilder.mkCreator(10L, "C", false, Status.SUCCESS);
            Customer cu = TransactionTestDataBuilder.mkCustomer(1L, "A", "N", "a@ex.com");
            Course co = TransactionTestDataBuilder.mkCourse(100L, "Course", 0, null, cr);
            Enrollment en = TransactionTestDataBuilder.mkEnrollment(500L, cu, co, true);
            CustomerTransaction tx = TransactionTestDataBuilder.mkTx(9002L, en, 0, 0, 0, "SUCCESS");

            when(transactionRepository.findById(9002L)).thenReturn(Optional.of(tx));

            TransactionDetailDto dto = service.getTransactionDetail(9002L);
            assertEquals(0.0, dto.getAdminPercentage());
            assertEquals(0.0, dto.getCreatorPercentage());
        }

        @Test
        void should_throw_when_not_found() {
            when(transactionRepository.findById(404L)).thenReturn(Optional.empty());
            RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getTransactionDetail(404L));
            assertTrue(ex.getMessage().contains("Transaction not found"));
        }
    }

    // ======= getRevenueReport() =======
    @Nested
    @DisplayName("getRevenueReport (BR-10, BR-03)")
    class GetRevenueReport {
        @TestFactory
        @DisplayName("Data-driven via revenue_report_cases.csv")
        Stream<DynamicTest> revenue_cases() throws Exception {
            return TestDataReader.readAsStream("/testdata/revenue_report_cases.csv").map(row -> {
                String caseId = row.get("caseId");
                String periodType = row.get("periodType");
                LocalDateTime start = LocalDateTime.parse(row.get("startDate"));
                LocalDateTime end = LocalDateTime.parse(row.get("endDate"));
                long success = Long.parseLong(row.get("successCount"));
                long failed = Long.parseLong(row.get("failedCount"));
                long pending = Long.parseLong(row.get("pendingCount"));
                double totalRevenue = Double.parseDouble(row.get("totalRevenue"));   // SUCCESS only (BR-10)
                double adminRevenue = Double.parseDouble(row.get("adminRevenue"));
                double creatorRevenue = Double.parseDouble(row.get("creatorRevenue"));
                double expectSuccessRate = Double.parseDouble(row.get("expectSuccessRate"));
                double expectAvg = Double.parseDouble(row.get("expectAvgValue"));
                boolean validRange = Boolean.parseBoolean(row.get("validRange"));

                return DynamicTest.dynamicTest("Case " + caseId, () -> {
                    if (!validRange) {
                        assertThrows(IllegalArgumentException.class,
                                () -> service.getRevenueReport(periodType, start, end));
                        return;
                    }

                    // Arrange repo mocks
                    when(transactionRepository.count()).thenReturn(success + failed + pending);
                    when(transactionRepository.findAllInDateRange(any(), any()))
                            .thenReturn(Collections.emptyList()); // chỉ để log debug
                    when(transactionRepository.getTotalRevenue(start, end)).thenReturn(totalRevenue);
                    when(transactionRepository.getAdminRevenue(start, end)).thenReturn(adminRevenue);
                    when(transactionRepository.getCreatorRevenue(start, end)).thenReturn(creatorRevenue);

                    when(transactionRepository.countByStatusAndDateRange("SUCCESS", start, end)).thenReturn(success);
                    when(transactionRepository.countByStatusAndDateRange("FAILED", start, end)).thenReturn(failed);
                    when(transactionRepository.countByStatusAndDateRange("PENDING", start, end)).thenReturn(pending);

                    Page<CourseRevenueProjection> topCourses = new PageImpl<>(
                            List.of(mkCourseProj(100L, "Course A", "img", 2_000_000, 120)),
                            PageRequest.of(0,10), 1);
                    Page<CreatorRevenueProjection> topCreators = new PageImpl<>(
                            List.of(mkCreatorProj(10L, "Creator A", 3_000_000, 3)),
                            PageRequest.of(0,10), 1);

                    when(transactionRepository.getTopCoursesByRevenue(start, end, PageRequest.of(0,10)))
                            .thenReturn(topCourses);
                    when(transactionRepository.getTopCreatorsByRevenue(start, end, PageRequest.of(0,10)))
                            .thenReturn(topCreators);

                    // Act
                    RevenueReportDto dto = service.getRevenueReport(periodType, start, end);

                    // Assert (BR-10: doanh thu chỉ từ SUCCESS)
                    assertEquals(totalRevenue, dto.getTotalRevenue());
                    assertEquals(adminRevenue, dto.getAdminRevenue());
                    assertEquals(creatorRevenue, dto.getCreatorRevenue());

                    long total = success + failed + pending;
                    assertEquals(total, dto.getTotalTransactions());
                    assertEquals(success, dto.getSuccessfulTransactions());
                    assertEquals(failed, dto.getFailedTransactions());
                    assertEquals(pending, dto.getPendingTransactions());

                    assertEquals(expectSuccessRate, dto.getSuccessRate(), 0.0001);
                    assertEquals(expectAvg, dto.getAverageTransactionValue(), 0.0001);

                    assertTrue(dto.getTopCourses().size() <= 10);
                    assertTrue(dto.getTopCreators().size() <= 10);
                });
            });
        }
    }

    // ======= getFailedTransactions() =======
    @Nested
    @DisplayName("getFailedTransactions")
    class GetFailedTransactions {

        @Test
        void should_return_only_failed_status() {
            Creator cr = TransactionTestDataBuilder.mkCreator(10L, "C", false, Status.SUCCESS);
            Customer cu = TransactionTestDataBuilder.mkCustomer(1L, "A", "N", "a@ex.com");
            Course co = TransactionTestDataBuilder.mkCourse(100L, "Course", 1_000_000, "img", cr);
            Enrollment en = TransactionTestDataBuilder.mkEnrollment(500L, cu, co, true);

            List<CustomerTransaction> failed = List.of(
                    TransactionTestDataBuilder.mkTx(1L, en, 1_000_000, 200_000, 800_000, "FAILED"),
                    TransactionTestDataBuilder.mkTx(2L, en, 1_000_000, 200_000, 800_000, "FAILED")
            );
            when(transactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .then(inv -> new PageImpl<>(failed, inv.getArgument(1), failed.size()));

            Page<TransactionListDto> page = service.getFailedTransactions(PageRequest.of(0,10));
            assertEquals(2, page.getContent().size());
            assertTrue(page.getContent().stream().allMatch(d -> "FAILED".equals(d.getStatus())));
        }
    }

    // ======= freeze/unfreeze (BR-19) =======
    @Nested
    @DisplayName("freeze/unfreeze (BR-19)")
    class FreezeUnfreeze {

        @Test
        void freeze_should_set_ban_true_and_status_under_review_and_log() {
            Creator cr = TransactionTestDataBuilder.mkCreator(10L, "Creator A", false, Status.SUCCESS);
            when(validationResources.validateCreatorExists(10L)).thenReturn(cr);

            service.freezeCreatorRevenue(10L, "fraud suspicion", "admin@jaen.dev");

            assertTrue(cr.isBan());
            assertEquals(Status.UNDER_REVIEW, cr.getStatus());
            verify(creatorRepository).save(cr);
            verify(auditLogService).logAction(
                    eq("FREEZE_REVENUE"), eq(10L), eq("admin@jaen.dev"),
                    contains("Reason: fraud suspicion"));
        }

        @Test
        void unfreeze_should_set_ban_false_and_status_success_and_log() {
            Creator cr = TransactionTestDataBuilder.mkCreator(11L, "Creator B", true, Status.UNDER_REVIEW);
            when(validationResources.validateCreatorExists(11L)).thenReturn(cr);

            service.unfreezeCreatorRevenue(11L, "admin@jaen.dev");

            assertFalse(cr.isBan());
            assertEquals(Status.SUCCESS, cr.getStatus());
            verify(creatorRepository).save(cr);
            verify(auditLogService).logAction(
                    eq("UNFREEZE_REVENUE"), eq(11L), eq("admin@jaen.dev"),
                    eq("Revenue unfrozen"));
        }
    }
}
