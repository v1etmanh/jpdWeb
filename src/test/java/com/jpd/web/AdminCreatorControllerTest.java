package com.jpd.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpd.web.controller.admin.AdminCreatorController;
import com.jpd.web.dto.AdminCreatorDetailDto;
import com.jpd.web.dto.AdminCreatorListDto;
import com.jpd.web.dto.CertificateApprovalDto;
import com.jpd.web.filter.CreatorFilter;
import com.jpd.web.model.AuditLog;
import com.jpd.web.model.Report;
import com.jpd.web.model.Status;
import com.jpd.web.repository.CustomerRepository;
import com.jpd.web.service.AdminCreatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCreatorController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCreatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminCreatorService adminCreatorService;

    @Autowired
    private CreatorFilter creatorFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private Jwt mockJwt;
    private CertificateApprovalDto certificateApprovalDto;

    @TestConfiguration
    static class MockConfig {

        @Bean
        public AdminCreatorService adminCreatorService() {
            return Mockito.mock(AdminCreatorService.class);
        }
        @Bean
        public CustomerRepository customerRepository() {
            return Mockito.mock(CustomerRepository.class);
        }

        @Bean
        public CreatorFilter creatorFilter() {
            return Mockito.mock(CreatorFilter.class);
        }
    }

    @BeforeEach
    void setup() {
        mockJwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("email", "admin@example.com")
                .build();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(mockJwt));
        SecurityContextHolder.setContext(context);

        certificateApprovalDto = new CertificateApprovalDto();
        certificateApprovalDto.setCreatorId(101L);
        certificateApprovalDto.setFullName("Nguyen Van A");
        certificateApprovalDto.setCertificateUrls(List.of(
                "https://example.com/cert1.png",
                "https://example.com/cert2.png"
        ));
        certificateApprovalDto.setSubmittedAt(new Date(123123182947128L));
        certificateApprovalDto.setStatus(Status.PENDING);
        certificateApprovalDto.setAdminNote(null);
    }

    @Test
    void testGetCreatorList() throws Exception {
        AdminCreatorListDto dto = new AdminCreatorListDto();
        Page<AdminCreatorListDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);

        when(adminCreatorService.getCreatorList(any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/creators")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void testGetCreatorDetail() throws Exception {
        AdminCreatorDetailDto dto = new AdminCreatorDetailDto();
        when(adminCreatorService.getCreatorDetail(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/creators/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnPendingCertificates() throws Exception {
        when(adminCreatorService.getPendingCertificates())
                .thenReturn(List.of(certificateApprovalDto));

        mockMvc.perform(get("/api/admin/creators/pending-certificates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creatorId").value(101))
                .andExpect(jsonPath("$[0].fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$[0].certificateUrls[0]").value("https://example.com/cert1.png"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(adminCreatorService).getPendingCertificates();
    }

    @Test
    void testApproveCertificate() throws Exception {
        Mockito.doNothing().when(adminCreatorService)
                .approveCertificate(eq(1L), eq("admin@example.com"), eq("Looks good"));

        mockMvc.perform(post("/api/admin/creators/1/approve-certificate")
                        .param("adminNote", "Looks good")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Certificate approved successfully"));
    }

    @Test
    void testRejectCertificate() throws Exception {
        Map<String, String> body = Map.of("reason", "Invalid document");

        Mockito.doNothing().when(adminCreatorService)
                .rejectCertificate(eq(1L), eq("Invalid document"), eq("admin@example.com"));

        mockMvc.perform(post("/api/admin/creators/1/reject-certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Certificate rejected"));
    }

    @Test
    void testWarnCreator() throws Exception {
        Mockito.doNothing().when(adminCreatorService)
                .warnCreator(eq(1L), eq("Violation of policy"), eq("admin@example.com"));

        mockMvc.perform(post("/api/admin/creators/1/warn")
                        .param("reason", "Violation of policy")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Warning issued successfully"));
    }

    @Test
    void testBanCreator() throws Exception {
        Map<String, String> body = Map.of(
                "reason", "Serious violation",
                "durationDays", "7"
        );

        Mockito.doNothing().when(adminCreatorService)
                .banCreator(eq(1L), eq("Serious violation"), eq(7), eq("admin@example.com"));

        mockMvc.perform(post("/api/admin/creators/1/ban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Creator banned successfully"));
    }

    @Test
    void testUnbanCreator() throws Exception {
        Map<String, String> body = Map.of("reason", "Ban period expired");

        Mockito.doNothing().when(adminCreatorService)
                .unbanCreator(eq(1L), eq("Ban period expired"), eq("admin@example.com"));

        mockMvc.perform(post("/api/admin/creators/1/unban")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(mockJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Creator unbanned successfully"));
    }

    @Test
    void testGetCreatorViolations() throws Exception {
        Report report = new Report();
        when(adminCreatorService.getCreatorViolationHistory(1L))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/api/admin/creators/1/violations"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetCreatorAuditLogs() throws Exception {
        AuditLog log = new AuditLog();
        when(adminCreatorService.getCreatorAuditLog(1L))
                .thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/creators/1/audit-logs"))
                .andExpect(status().isOk());
    }
}
