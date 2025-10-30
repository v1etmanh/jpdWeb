package com.jpd.web.controller.admin;

import com.jpd.web.dto.*;
import com.jpd.web.model.AuditLog;
import com.jpd.web.model.Report;
import com.jpd.web.model.Status;
import com.jpd.web.service.AdminCreatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/creators")
@RequiredArgsConstructor
@Slf4j
public class AdminCreatorController {

    private final AdminCreatorService adminCreatorService;

    // List creators
    @GetMapping
    public ResponseEntity<Page<AdminCreatorListDto>> getCreatorList(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AdminCreatorListDto> result = adminCreatorService.getCreatorList(status, search, page, size);
        return ResponseEntity.ok(result);
    }

    // Get creator detail
    @GetMapping("/{creatorId}")
    public ResponseEntity<AdminCreatorDetailDto> getCreatorDetail(@PathVariable Long creatorId) {
        AdminCreatorDetailDto dto = adminCreatorService.getCreatorDetail(creatorId);
        return ResponseEntity.ok(dto);
    }

    // Get pending certificates
    @GetMapping("/pending-certificates")
    public ResponseEntity<List<CertificateApprovalDto>> getPendingCertificates() {
        List<CertificateApprovalDto> pending = adminCreatorService.getPendingCertificates();
        return ResponseEntity.ok(pending);
    }

    // Approve certificate
    @PostMapping("/{creatorId}/approve-certificate")
    public ResponseEntity<Map<String, String>> approveCertificate(
            @PathVariable Long creatorId,
            @RequestParam("adminNote")String adminNote,
            @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");


        adminCreatorService.approveCertificate(creatorId, adminEmail, adminNote);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Certificate approved successfully");
        return ResponseEntity.ok(response);
    }

    // Reject certificate
    @PostMapping("/{creatorId}/reject-certificate")
    public ResponseEntity<Map<String, String>> rejectCertificate(
            @PathVariable Long creatorId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        String reason = request.get("reason");

        adminCreatorService.rejectCertificate(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Certificate rejected");
        return ResponseEntity.ok(response);
    }

    // Warn creator
    @PostMapping("/{creatorId}/warn")
    public ResponseEntity<Map<String, String>> warnCreator(
            @PathVariable Long creatorId,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");


        adminCreatorService.warnCreator(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Warning issued successfully");
        return ResponseEntity.ok(response);
    }

    // Ban creator
    @PostMapping("/{creatorId}/ban")
    public ResponseEntity<Map<String, String>> banCreator(
            @PathVariable Long creatorId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        String reason = request.get("reason");
        Integer durationDays = request.get("durationDays") != null ? Integer.parseInt(request.get("durationDays"))
                : null;

        adminCreatorService.banCreator(creatorId, reason, durationDays, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Creator banned successfully");
        return ResponseEntity.ok(response);
    }

    // Unban creator
    @PostMapping("/{creatorId}/unban")
    public ResponseEntity<Map<String, String>> unbanCreator(
            @PathVariable Long creatorId,
            @RequestBody(required = false) Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt.getClaimAsString("email");
        String reason = request != null ? request.get("reason") : "Unbanned by admin";

        adminCreatorService.unbanCreator(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Creator unbanned successfully");
        return ResponseEntity.ok(response);
    }

    // Get violation history
    @GetMapping("/{creatorId}/violations")
    public ResponseEntity<List<Report>> getCreatorViolations(@PathVariable Long creatorId) {
        List<Report> violations = adminCreatorService.getCreatorViolationHistory(creatorId);
        return ResponseEntity.ok(violations);
    }

    // Get audit logs
    @GetMapping("/{creatorId}/audit-logs")
    public ResponseEntity<List<AuditLog>> getCreatorAuditLogs(@PathVariable Long creatorId) {
        List<AuditLog> logs = adminCreatorService.getCreatorAuditLog(creatorId);
        return ResponseEntity.ok(logs);
    }
}
