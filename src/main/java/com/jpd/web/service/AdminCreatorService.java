package com.jpd.web.service;

import com.jpd.web.dto.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.*;
import com.jpd.web.service.utils.ValidationResources;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminCreatorService {

    private final CreatorRepository creatorRepository;
    private final ReportRepository reportRepository;
    private final AuditLogService auditLogService;
    private final CreatorWarningRepository warningRepository;
    private final ValidationResources validationResources;

    // List creators with filters
    public Page<AdminCreatorListDto> getCreatorList(Status status, String searchKeyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Creator> creators;

        // Filter by status
        if (status != null) {
            creators = creatorRepository.findAllByStatus(status);
        } else {
            creators = creatorRepository.findAll();
        }

        // Search by name
        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            creators = creators.stream()
                    .filter(c -> c.getFullName() != null &&
                            c.getFullName().toLowerCase().contains(searchKeyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Convert to DTO
        List<AdminCreatorListDto> dtos = creators.stream()
                .map(this::toAdminCreatorListDto)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<AdminCreatorListDto> pageContent = dtos.isEmpty() ? List.of() : dtos.subList(start, end);

        return new PageImpl<>(pageContent, pageable, dtos.size());
    }

    // Get creator detail
    public AdminCreatorDetailDto getCreatorDetail(Long creatorId) {
        Creator creator = validationResources.validateCreatorExists(creatorId);
        return toAdminCreatorDetailDto(creator);
    }

    // Get pending certificates
    public List<CertificateApprovalDto> getPendingCertificates() {
        List<Creator> pendingCreators = creatorRepository.findAllByStatus(Status.PENDING);
        return pendingCreators.stream()
                .map(this::toCertificateApprovalDto)
                .collect(Collectors.toList());
    }

    // Approve certificate (BR-19)
    @Transactional
    public void approveCertificate(Long creatorId, String adminEmail, String adminNote) {
        Creator creator = validationResources.validateCreatorExists(creatorId);

        if (creator.getStatus() != Status.PENDING) {
            throw new IllegalStateException("Creator certificate is not pending approval");
        }

        creator.setStatus(Status.SUCCESS);
        creatorRepository.save(creator);

        auditLogService.logAction(
                "APPROVE_CERT",
                creatorId,
                adminEmail,
                "Certificate approved. Note: " + (adminNote != null ? adminNote : "N/A"));

        log.info("Certificate approved for creator {} by admin {}", creatorId, adminEmail);
    }

    // Reject certificate
    @Transactional
    public void rejectCertificate(Long creatorId, String reason, String adminEmail) {
        Creator creator = validationResources.validateCreatorExists(creatorId);

        if (creator.getStatus() != Status.PENDING) {
            throw new IllegalStateException("Creator certificate is not pending approval");
        }

        creator.setStatus(Status.REJECTED);
        creatorRepository.save(creator);

        auditLogService.logAction(
                "REJECT_CERT",
                creatorId,
                adminEmail,
                "Certificate rejected. Reason: " + reason);

        log.info("Certificate rejected for creator {} by admin {}", creatorId, adminEmail);
    }

    // Warn creator (BR-10: Check if ≥3 warnings in 90 days → suspend)
    @Transactional
    public void warnCreator(Long creatorId, String reason, String adminEmail) {
        Creator creator = validationResources.validateCreatorExists(creatorId);

        // Create warning record
        CreatorWarning warning = CreatorWarning.builder()
                .creator(creator)
                .reason(reason)
                .issuedByAdmin(adminEmail)
                .isActive(true)
                .build();
        warningRepository.save(warning);

        // Update warning count
        int newWarningCount = (creator.getWarningCount() != null ? creator.getWarningCount() : 0) + 1;
        creator.setWarningCount(newWarningCount);

        // Check if should auto-suspend (≥3 warnings in last 90 days)
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        Long recentWarnings = warningRepository.countByCreatorAndIssuedAtAfter(creator, ninetyDaysAgo);

        if (recentWarnings >= 3) {
            creator.setStatus(Status.SUSPENDED);
            log.warn("Creator {} has 3 or more warnings in 90 days, auto-suspended", creatorId);
        }

        creatorRepository.save(creator);

        auditLogService.logAction("WARN_CREATOR", creatorId, adminEmail, reason);
        log.info("Warning issued to creator {} by admin {}", creatorId, adminEmail);
    }

    // Ban creator
    @Transactional
    public void banCreator(Long creatorId, String reason, Integer durationDays, String adminEmail) {
        Creator creator = validationResources.validateCreatorExists(creatorId);

        creator.setBan(true);

        if (durationDays != null && durationDays > 0) {
            // Temporary ban
            LocalDateTime banUntil = LocalDateTime.now().plusDays(durationDays);
            creator.setBannedUntil(Date.valueOf(banUntil.toLocalDate()));
            creator.setStatus(Status.BANNED);
        } else {
            // Permanent ban
            creator.setStatus(Status.BANNED);
            creator.setBannedUntil(null);
        }

        // Ban all courses
        if (creator.getCourses() != null) {
            creator.getCourses().forEach(course -> course.setBan(true));
        }

        creatorRepository.save(creator);

        auditLogService.logAction(
                "BAN_CREATOR",
                creatorId,
                adminEmail,
                String.format("Creator banned. Duration: %d days. Reason: %s",
                        durationDays != null ? durationDays : -1, reason));

        log.info("Creator {} banned by admin {}", creatorId, adminEmail);
    }

    // Unban creator
    @Transactional
    public void unbanCreator(Long creatorId, String reason, String adminEmail) {
        Creator creator = validationResources.validateCreatorExists(creatorId);

        creator.setBan(false);
        creator.setBannedUntil(null);
        creator.setStatus(Status.SUCCESS);

        // Unban all courses
        if (creator.getCourses() != null) {
            creator.getCourses().forEach(course -> course.setBan(false));
        }

        creatorRepository.save(creator);

        auditLogService.logAction("UNBAN_CREATOR", creatorId, adminEmail, reason);
        log.info("Creator {} unbanned by admin {}", creatorId, adminEmail);
    }

    // Get violation history
    public List<Report> getCreatorViolationHistory(Long creatorId) {
        return reportRepository.findByCreator_CreatorId(creatorId);
    }

    // Get audit logs for creator
    public List<AuditLog> getCreatorAuditLog(Long creatorId) {
        return auditLogService.getLogsByCreator(creatorId);
    }

    // Transform methods
    private AdminCreatorListDto toAdminCreatorListDto(Creator creator) {
        int totalCourses = creator.getCourses() != null ? creator.getCourses().size() : 0;
        int totalStudents = creatorRepository.countTotalStudentsByCreatorId(creator.getCreatorId());
        Double avgRating = creatorRepository.getAverageRatingByCreatorId(creator.getCreatorId());

        return AdminCreatorListDto.builder()
                .creatorId(creator.getCreatorId())
                .fullName(creator.getFullName())
                .email(creator.getCustomer() != null ? creator.getCustomer().getEmail() : null)
                .imageUrl(creator.getImageUrl())
                .status(creator.getStatus())
                .balance(creator.getBalance())
                .totalCourses(totalCourses)
                .totalStudents(totalStudents)
                .avgRating(avgRating != null ? avgRating : 0.0)
                .warningCount(creator.getWarningCount() != null ? creator.getWarningCount() : 0)
                .createDate(creator.getCreateDate())
                .build();
    }

    private AdminCreatorDetailDto toAdminCreatorDetailDto(Creator creator) {
        int totalCourses = creator.getCourses() != null ? creator.getCourses().size() : 0;
        int totalStudents = creatorRepository.countTotalStudentsByCreatorId(creator.getCreatorId());
        Double avgRating = creatorRepository.getAverageRatingByCreatorId(creator.getCreatorId());

        // Recent reports (last 10)
        List<Report> recentReports = reportRepository.findByCreator_CreatorId(creator.getCreatorId())
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        List<AdminCreatorDetailDto.ReportSummaryDto> reportDtos = recentReports.stream()
                .map(r -> AdminCreatorDetailDto.ReportSummaryDto.builder()
                        .reportId(r.getReportId())
                        .reportType(r.getType() != null ? r.getType().name() : null)
                        .detail(r.getDetail())
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate() : null)
                        .reviewedAt(r.getReviewedAt() != null ? r.getReviewedAt().toLocalDate() : null)
                        .build())
                .collect(Collectors.toList());

        // Recent courses (top 5)
        List<CourseCardDto> recentCourses = creator.getCourses() != null ? creator.getCourses().stream()
                .limit(5)
                .map(this::toCourseCardDto)
                .collect(Collectors.toList()) : List.of();

        return AdminCreatorDetailDto.builder()
                .creatorId(creator.getCreatorId())
                .fullName(creator.getFullName())
                .email(creator.getCustomer() != null ? creator.getCustomer().getEmail() : null)
                .phone(creator.getMobiPhone())
                .titleSelf(creator.getTitleSelf())
                .imageUrl(creator.getImageUrl())
                .status(creator.getStatus())
                .certificateUrls(creator.getCertificateUrl())
                .paymentEmail(creator.getPaymentEmail())
                .balance(creator.getBalance())
                .totalRevenue(creator.getBalance()) // Simplified
                .totalCourses(totalCourses)
                .totalStudents(totalStudents)
                .avgRating(avgRating != null ? avgRating : 0.0)
                .warningCount(creator.getWarningCount() != null ? creator.getWarningCount() : 0)
                .reputationScore(creator.getReputationScore())
                .isBanned(creator.isBan())
                .bannedUntil(creator.getBannedUntil())
                .recentReports(reportDtos)
                .recentCourses(recentCourses)
                .createDate(creator.getCreateDate())
                .build();
    }

    private CertificateApprovalDto toCertificateApprovalDto(Creator creator) {
        return CertificateApprovalDto.builder()
                .creatorId(creator.getCreatorId())
                .fullName(creator.getFullName())
                .certificateUrls(creator.getCertificateUrl())
                .submittedAt(creator.getCreateDate())
                .status(creator.getStatus())
                .build();
    }

    private CourseCardDto toCourseCardDto(Course course) {
        int studentCount = course.getEnrollments() != null ? course.getEnrollments().size() : 0;
        double totalRevenue = course.getPrice() * studentCount;

        double avgRating = 0.0;
        int reviewCount = 0;
        if (course.getEnrollments() != null) {
            for (Enrollment e : course.getEnrollments()) {
                if (e.getFeedback() != null) {
                    avgRating += e.getFeedback().getRate();
                    reviewCount++;
                }
            }
            if (reviewCount > 0)
                avgRating /= reviewCount;
        }

        return new CourseCardDto(
                course.getCourseId(),
                course.getName(),
                course.getCreatedAt(),
                studentCount,
                reviewCount,
                totalRevenue,
                avgRating,
                course.getUrlImg(),
                course.getAccessMode(),
                course.isPublic(),
                course.getJoinKey());
    }
}
