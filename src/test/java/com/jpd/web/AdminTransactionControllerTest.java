package com.jpd.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.controller.admin.AdminTransactionController;
import com.jpd.web.dto.RevenueReportDto;
import com.jpd.web.dto.TransactionDetailDto;
import com.jpd.web.dto.TransactionFilterDto;
import com.jpd.web.dto.TransactionListDto;
import com.jpd.web.filter.CreatorFilter;
import com.jpd.web.model.PaymentMethod;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.AdminCreatorService;
import com.jpd.web.service.AdminTransactionService;
import com.jpd.web.service.ExcelExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminTransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminTransactionService transactionService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private CreatorFilter creatorFilter;

    private TransactionListDto mockTransaction;

    private TransactionDetailDto mockDetail;

    private RevenueReportDto mockReport;

    private TransactionListDto failedTransaction;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class MockConfig {

        @Bean
        public AdminTransactionService adminTransactionService() {
            return Mockito.mock(AdminTransactionService.class);
        }

        @Bean
        public ExcelExportService excelExportService() {
            return Mockito.mock(ExcelExportService.class);
        }

        @Bean
        public CreatorFilter creatorFilter() {
            return Mockito.mock(CreatorFilter.class);
        }

        @Bean
        public AdminCreatorService adminCreatorService() {
            return Mockito.mock(AdminCreatorService.class);
        }

        @Bean
        public CustomerRepository customerRepository() {
            return Mockito.mock(CustomerRepository.class);
        }
    }


    @BeforeEach
    void setup() {
        mockTransaction = TransactionListDto.builder()
                .transactionId(1L)
                .amount(250000.0)
                .currency("VND")
                .status("SUCCESS")
                .customerName("Nguyen Van A")
                .paymentMethod(PaymentMethod.PAYPAL)
                .createdAt(LocalDateTime.now())
                .build();

        mockDetail = TransactionDetailDto.builder()
                .transactionId(1L)
                .amount(250000.0)
                .status("SUCCESS")
                .paymentMethod(PaymentMethod.PAYPAL)
                .createdAt(LocalDateTime.now())
                .build();

        mockReport = RevenueReportDto.builder()
                .periodType("MONTH")
                .startDate(LocalDateTime.of(2024, 10, 1, 0, 0))
                .endDate(LocalDateTime.of(2024, 10, 31, 23, 59))
                .totalRevenue(5_000_000.0)
                .adminRevenue(1_000_000.0)
                .creatorRevenue(4_000_000.0)
                .totalTransactions(100L)
                .successfulTransactions(95L)
                .failedTransactions(3L)
                .pendingTransactions(2L)
                .averageTransactionValue(50_000.0)
                .successRate(95.0)
                .topCourses(List.of(
                        RevenueReportDto.CourseRevenueSummary.builder()
                                .courseId(1L)
                                .courseName("Spring Boot Mastery")
                                .totalRevenue(1_500_000.0)
                                .enrollmentCount(50L)
                                .imageUrl("https://example.com/spring.png")
                                .build()
                ))
                .topCreators(List.of(
                        RevenueReportDto.CreatorRevenueSummary.builder()
                                .creatorId(10L)
                                .creatorName("Tran Van B")
                                .totalRevenue(2_000_000.0)
                                .courseCount(3L)
                                .build()
                ))
                .build();

        failedTransaction = TransactionListDto.builder()
                .transactionId(999L)
                .amount(120000.0)
                .currency("VND")
                .status("FAILED")
                .customerName("Nguyen Van C")
                .creatorName("Tran Van D")
                .paymentMethod(PaymentMethod.PAYPAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnTransactionList() throws Exception {
        when(transactionService.getTransactionList(any(TransactionFilterDto.class)))
                .thenReturn(new PageImpl<>(List.of(mockTransaction), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/admin/transactions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customerName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.content[0].amount").value(250000.0))
                .andDo(res ->
                        System.out.println("Response JSON: " + res.getResponse().getContentAsString())
                );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnTransactionDetail() throws Exception {
        when(transactionService.getTransactionDetail(1L)).thenReturn(mockDetail);

        mockMvc.perform(get("/api/admin/transactions/{transactionId}", 1)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(250000.0))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andDo(res ->
                        System.out.println("Response JSON: " + res.getResponse().getContentAsString())
                );
    }

    /**
     * Test: Lấy báo cáo doanh thu theo tháng
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnRevenueReportForMonth() throws Exception {
        when(transactionService.getRevenueReport(eq("MONTH"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockReport);

        mockMvc.perform(get("/api/admin/transactions/revenue-report")
                        .param("period", "MONTH")
                        .param("month", "10")
                        .param("year", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodType").value("MONTH"))
                .andExpect(jsonPath("$.data.totalRevenue").value(5_000_000.0))
                .andExpect(jsonPath("$.data.adminRevenue").value(1_000_000.0))
                .andExpect(jsonPath("$.data.creatorRevenue").value(4_000_000.0))
                .andExpect(jsonPath("$.data.totalTransactions").value(100))
                .andExpect(jsonPath("$.data.successRate").value(95.0))
                .andExpect(jsonPath("$.data.topCourses[0].courseName").value("Spring Boot Mastery"))
                .andExpect(jsonPath("$.data.topCourses[0].totalRevenue").value(1_500_000.0))
                .andExpect(jsonPath("$.data.topCreators[0].creatorName").value("Tran Van B"))
                .andExpect(jsonPath("$.data.topCreators[0].courseCount").value(3))
                .andDo(result -> System.out.println("Response JSON: " + result.getResponse().getContentAsString()));
    }

    /**
     * Lấy báo cáo doanh thu theo quý
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnRevenueReportForQuarter() throws Exception {
        mockReport.setPeriodType("QUARTER");
        mockReport.setTotalRevenue(12_000_000.0);
        mockReport.setSuccessRate(96.5);

        when(transactionService.getRevenueReport(eq("QUARTER"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockReport);

        mockMvc.perform(get("/api/admin/transactions/revenue-report")
                        .param("period", "QUARTER")
                        .param("quarter", "3")
                        .param("year", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodType").value("QUARTER"))
                .andExpect(jsonPath("$.data.totalRevenue").value(12_000_000.0))
                .andExpect(jsonPath("$.data.successRate").value(96.5))
                .andExpect(jsonPath("$.data.topCreators[0].creatorName").value("Tran Van B"))
                .andDo(result -> System.out.println("QUARTER JSON: " + result.getResponse().getContentAsString()));
    }

    /**
     *  Lấy báo cáo doanh thu theo năm
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnRevenueReportForYear() throws Exception {
        mockReport.setPeriodType("YEAR");
        mockReport.setTotalRevenue(50_000_000.0);
        mockReport.setSuccessRate(92.5);

        when(transactionService.getRevenueReport(eq("YEAR"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockReport);

        mockMvc.perform(get("/api/admin/transactions/revenue-report")
                        .param("period", "YEAR")
                        .param("year", "2024")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodType").value("YEAR"))
                .andExpect(jsonPath("$.data.totalRevenue").value(50_000_000.0))
                .andExpect(jsonPath("$.data.successRate").value(92.5))
                .andExpect(jsonPath("$.data.topCreators[0].creatorName").value("Tran Van B"))
                .andDo(result -> System.out.println("Response JSON: " + result.getResponse().getContentAsString()));
    }
    /**
     *  Lấy báo cáo doanh thu theo khoảng thời gian custom
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnRevenueReportForCustomRange() throws Exception {
        mockReport.setPeriodType("CUSTOM");
        mockReport.setTotalRevenue(3_000_000.0);
        mockReport.setSuccessRate(90.0);

        when(transactionService.getRevenueReport(eq("CUSTOM"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(mockReport);

        mockMvc.perform(get("/api/admin/transactions/revenue-report")
                        .param("period", "CUSTOM")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-31T23:59:59")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodType").value("CUSTOM"))
                .andExpect(jsonPath("$.data.totalRevenue").value(3_000_000.0))
                .andExpect(jsonPath("$.data.successRate").value(90.0))
                .andDo(result -> System.out.println("CUSTOM JSON: " + result.getResponse().getContentAsString()));
    }
    /**
     * Lấy danh sách giao dịch thất bại
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnFailedTransactions() throws Exception {
        // Mock service trả về danh sách 1 giao dịch thất bại
        when(transactionService.getFailedTransactions(any()))
                .thenReturn(new PageImpl<>(List.of(failedTransaction), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/transactions/failed")
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.content[0].customerName").value("Nguyen Van C"))
                .andExpect(jsonPath("$.data.content[0].amount").value(120000.0))
                .andExpect(jsonPath("$.data.content[0].creatorName").value("Tran Van D"))
                .andDo(result ->
                        System.out.println("FAILED TRANSACTIONS JSON: " + result.getResponse().getContentAsString()));
    }

    /**
     *Freeze creator revenue
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldFreezeCreatorRevenueSuccessfully() throws Exception {
        Map<String, Object> request = Map.of(
                "creatorId", 101,
                "reason", "violate policy"
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("email", "admin@example.com")
                .build();
        context.setAuthentication(new JwtAuthenticationToken(jwt));
        SecurityContextHolder.setContext(context);

        doNothing().when(transactionService)
                .freezeCreatorRevenue(eq(101L), eq("violate policy"), eq("admin@example.com"));


        mockMvc.perform(post("/api/admin/transactions/freeze-creator-revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(() -> jwt.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Creator revenue frozen successfully"))
                .andExpect(jsonPath("$.data.creatorId").value("101"))
                .andDo(result -> System.out.println("FREEZE JSON RESPONSE: " + result.getResponse().getContentAsString()));

        verify(transactionService)
                .freezeCreatorRevenue(101L, "violate policy", "admin@example.com");
    }

    /**
     * Unfreeze creator revenue
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUnfreezeCreatorRevenueSuccessfully() throws Exception {
        Map<String, Object> request = Map.of(
                "creatorId", 202
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("email", "admin@example.com")
                .build();
        context.setAuthentication(new JwtAuthenticationToken(jwt));
        SecurityContextHolder.setContext(context);

        doNothing().when(transactionService)
                .unfreezeCreatorRevenue(eq(202L), eq("admin@example.com"));

        mockMvc.perform(post("/api/admin/transactions/unfreeze-creator-revenue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(() -> jwt.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Creator revenue unfrozen successfully"))
                .andExpect(jsonPath("$.data.creatorId").value("202"))
                .andDo(result -> System.out.println("UNFREEZE RESPONSE: " + result.getResponse().getContentAsString()));

        verify(transactionService).unfreezeCreatorRevenue(202L, "admin@example.com");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldExportMonthlyExcelSuccessfully() throws Exception {
        byte[] mockExcel = "Monthly Report Excel Data".getBytes(StandardCharsets.UTF_8);

        when(excelExportService.exportMonthlyReport(2, 2025))
                .thenReturn(mockExcel);

        mockMvc.perform(get("/api/admin/transactions/export/excel")
                        .param("month", "2")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename=\"transactions_month_2_2025.xlsx\"")))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(mockExcel))
                .andDo(result ->
                        System.out.println("Monthly Excel Response Bytes: " + result.getResponse().getContentAsString()));

        verify(excelExportService).exportMonthlyReport(2, 2025);
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldExportQuarterlyExcelSuccessfully() throws Exception {
        byte[] mockExcel = "Quarterly Report Excel Data".getBytes(StandardCharsets.UTF_8);

        when(excelExportService.exportQuarterlyReport(2, 2025))
                .thenReturn(mockExcel);

        mockMvc.perform(get("/api/admin/transactions/export/excel/quarterly")
                        .param("quarter", "2")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename=\"transactions_q2_2025.xlsx\"")))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(mockExcel))
                .andDo(result ->
                        System.out.println("Quarterly Excel Response Bytes: " + result.getResponse().getContentAsString()));

        verify(excelExportService).exportQuarterlyReport(2, 2025);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldExportYearlyExcelSuccessfully() throws Exception {
        byte[] mockExcel = "Yearly Report Excel Data".getBytes(StandardCharsets.UTF_8);

        when(excelExportService.exportYearlyReport(2025))
                .thenReturn(mockExcel);

        mockMvc.perform(get("/api/admin/transactions/export/excel/yearly")
                        .param("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename=\"transactions_year_2025.xlsx\"")))
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(mockExcel))
                .andDo(result ->
                        System.out.println("Yearly Excel Response Bytes: " + result.getResponse().getContentAsString()));

        verify(excelExportService).exportYearlyReport(2025);
    }
}
