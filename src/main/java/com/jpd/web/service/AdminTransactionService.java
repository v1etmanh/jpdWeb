package com.jpd.web.service;

import com.jpd.web.dto.*;
import com.jpd.web.model.*;
import com.jpd.web.repository.CreatorRepository;
import com.jpd.web.repository.CustomerTransactionRepository;
import com.jpd.web.repository.projection.CourseRevenueProjection;
import com.jpd.web.repository.projection.CreatorRevenueProjection;
import com.jpd.web.service.utils.ValidationResources;
import com.jpd.web.specification.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminTransactionService {

        private final CustomerTransactionRepository transactionRepository;
        private final CreatorRepository creatorRepository;
        private final ValidationResources validationResources;
        private final AuditLogService auditLogService;

        /**
         * Get paginated transaction list with dynamic filters using Specifications
         */
        public Page<TransactionListDto> getTransactionList(TransactionFilterDto filter) {
                // Build specification from filter
                Specification<CustomerTransaction> spec = TransactionSpecification.withFilters(filter);

                // Create sort
                Sort sort = Sort.by(
                                Sort.Direction.fromString(filter.getSortDirection()),
                                filter.getSortBy());

                // Create pageable
                Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

                // Execute query and map to DTO
                return transactionRepository.findAll(spec, pageable)
                                .map(this::toTransactionListDto);
        }

        /**
         * Get detailed transaction information
         */
        public TransactionDetailDto getTransactionDetail(Long transactionId) {
                CustomerTransaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

                return toTransactionDetailDto(transaction);
        }

        /**
         * Get revenue report for a specific period
         */
        public RevenueReportDto getRevenueReport(String periodType, LocalDateTime startDate, LocalDateTime endDate) {
            log.info("=== REVENUE REPORT DEBUG ===");
            log.info("Period: {}, Start: {}, End: {}", periodType, startDate, endDate);
            
            // CRITICAL: Check if dates are correct
            if (startDate.isAfter(endDate)) {
                log.error("Start date is after end date!");
                throw new IllegalArgumentException("Start date must be before end date");
            }
            
            // Debug: Check total data in DB
            long totalInDb = transactionRepository.count();
            log.info("Total transactions in database: {}", totalInDb);
            
            // Debug: Check data in date range
            List<CustomerTransaction> allInRange = transactionRepository
                    .findAllInDateRange(startDate, endDate);
            log.info("Transactions in date range: {}", allInRange.size());
            
            // Debug: Print first 3 transactions
            allInRange.stream().limit(3).forEach(t -> {
                log.info("Sample - ID: {}, Status: {}, Amount: {}, Created: {}", 
                        t.getTransactionID(), t.getStatus(), t.getAmount(), t.getCreatedAt());
            });
            
            // Calculate revenue statistics (SUCCESS only)
            Double totalRevenue = transactionRepository.getTotalRevenue(startDate, endDate);
            Double adminRevenue = transactionRepository.getAdminRevenue(startDate, endDate);
            Double creatorRevenue = transactionRepository.getCreatorRevenue(startDate, endDate);
            
            log.info("Revenue - Total: {}, Admin: {}, Creator: {}", 
                    totalRevenue, adminRevenue, creatorRevenue);
            
            // Count transactions by status
            Long successful = transactionRepository.countByStatusAndDateRange("SUCCESS", startDate, endDate);
            Long failed = transactionRepository.countByStatusAndDateRange("FAILED", startDate, endDate);
            Long pending = transactionRepository.countByStatusAndDateRange("PENDING", startDate, endDate);
            Long total = successful + failed + pending;
            
            log.info("Transactions - Success: {}, Failed: {}, Pending: {}, Total: {}", 
                    successful, failed, pending, total);
            
            // Check if no data found
            if (total == 0) {
                log.warn("No transactions found in the specified date range!");
                log.warn("Check if: 1) Data exists, 2) Date range is correct, 3) Timezone issues");
            }
            
            // Calculate metrics
            Double avgValue = total > 0 ? (totalRevenue / total) : 0.0;
            Double successRate = total > 0 ? (successful.doubleValue() / total * 100) : 0.0;
            
            // Get top courses and creators
            Page<CourseRevenueProjection> topCoursesPage = transactionRepository
                    .getTopCoursesByRevenue(startDate, endDate, PageRequest.of(0, 10));
            
            Page<CreatorRevenueProjection> topCreatorsPage = transactionRepository
                    .getTopCreatorsByRevenue(startDate, endDate, PageRequest.of(0, 10));
            
            log.info("Top courses found: {}", topCoursesPage.getTotalElements());
            log.info("Top creators found: {}", topCreatorsPage.getTotalElements());
            
            List<RevenueReportDto.CourseRevenueSummary> topCourses = topCoursesPage.getContent().stream()
                    .map(p -> RevenueReportDto.CourseRevenueSummary.builder()
                            .courseId(p.getCourseId())
                            .courseName(p.getCourseName())
                            .imageUrl(p.getImageUrl())
                            .totalRevenue(p.getTotalRevenue())
                            .enrollmentCount(p.getEnrollmentCount())
                            .build())
                    .collect(Collectors.toList());

            List<RevenueReportDto.CreatorRevenueSummary> topCreators = topCreatorsPage.getContent().stream()
                    .map(p -> RevenueReportDto.CreatorRevenueSummary.builder()
                            .creatorId(p.getCreatorId())
                            .creatorName(p.getCreatorName())
                            .totalRevenue(p.getTotalRevenue())
                            .courseCount(p.getCourseCount())
                            .build())
                    .collect(Collectors.toList());

            return RevenueReportDto.builder()
                    .periodType(periodType)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalRevenue(totalRevenue)
                    .adminRevenue(adminRevenue)
                    .creatorRevenue(creatorRevenue)
                    .totalTransactions(total)
                    .successfulTransactions(successful)
                    .failedTransactions(failed)
                    .pendingTransactions(pending)
                    .averageTransactionValue(avgValue)
                    .successRate(successRate)
                    .topCourses(topCourses)
                    .topCreators(topCreators)
                    .build();
        }

        /**
         * Get failed transactions
         */
        public Page<TransactionListDto> getFailedTransactions(Pageable pageable) {
                Page<CustomerTransaction> failedTransactions = transactionRepository.findAll(
                                (root, query, cb) -> cb.equal(root.get("status"), "FAILED"),
                                pageable);

                return failedTransactions.map(this::toTransactionListDto);
        }

        /**
         * Freeze creator revenue (prevent withdrawals)
         */
        @Transactional
        public void freezeCreatorRevenue(Long creatorId, String reason, String adminEmail) {
                Creator creator = validationResources.validateCreatorExists(creatorId);

                creator.setBan(true);
                creator.setStatus(Status.UNDER_REVIEW);
                creatorRepository.save(creator);

                auditLogService.logAction(
                                "FREEZE_REVENUE",
                                creatorId,
                                adminEmail,
                                "Revenue frozen. Reason: " + reason);

                log.info("Creator {} revenue frozen by admin {}", creatorId, adminEmail);
        }

        /**
         * Unfreeze creator revenue (allow withdrawals)
         */
        @Transactional
        public void unfreezeCreatorRevenue(Long creatorId, String adminEmail) {
                Creator creator = validationResources.validateCreatorExists(creatorId);

                creator.setBan(false);
                creator.setStatus(Status.SUCCESS);
                creatorRepository.save(creator);

                auditLogService.logAction(
                                "UNFREEZE_REVENUE",
                                creatorId,
                                adminEmail,
                                "Revenue unfrozen");

                log.info("Creator {} revenue unfrozen by admin {}", creatorId, adminEmail);
        }

        // Transform methods
        private TransactionListDto toTransactionListDto(CustomerTransaction transaction) {
                Enrollment enrollment = transaction.getEnrollment();
                Course course = enrollment.getCourse();
                Creator creator = course.getCreator();
                Customer customer = enrollment.getCustomer();

                return TransactionListDto.builder()
                                .transactionId(transaction.getTransactionID())
                                .amount(transaction.getAmount())
                                .currency(transaction.getCurrency())
                                .status(transaction.getStatus())
                                .customerName(customer.getGivenName() + " " + customer.getFamilyName())
                                .customerEmail(customer.getEmail())
                                .customerId(customer.getCustomerId())
                                .courseName(course.getName())
                                .courseId(course.getCourseId())
                                .creatorName(creator.getFullName())
                                .creatorId(creator.getCreatorId())
                                .adminGet(transaction.getAdminGet())
                                .creatorGet(transaction.getCreatorGet())
                                .paymentMethod(transaction.getPaymentMethod())
                                .paymentId(transaction.getPaymentId())
                                .createdAt(transaction.getCreatedAt())
                                .updatedAt(transaction.getUpdatedAt())
                                .build();
        }

        private TransactionDetailDto toTransactionDetailDto(CustomerTransaction transaction) {
                Enrollment enrollment = transaction.getEnrollment();
                Course course = enrollment.getCourse();
                Creator creator = course.getCreator();
                Customer customer = enrollment.getCustomer();

                double adminPercentage = transaction.getAmount() > 0
                                ? (transaction.getAdminGet() / transaction.getAmount() * 100)
                                : 0.0;
                double creatorPercentage = transaction.getAmount() > 0
                                ? (transaction.getCreatorGet() / transaction.getAmount() * 100)
                                : 0.0;

                return TransactionDetailDto.builder()
                                .transactionId(transaction.getTransactionID())
                                .amount(transaction.getAmount())
                                .currency(transaction.getCurrency())
                                .status(transaction.getStatus())
                                .content(transaction.getContent())
                                .paymentMethod(transaction.getPaymentMethod())
                                .paymentId(transaction.getPaymentId())
                                .createdAt(transaction.getCreatedAt())
                                .updatedAt(transaction.getUpdatedAt())
                                .adminGet(transaction.getAdminGet())
                                .creatorGet(transaction.getCreatorGet())
                                .adminPercentage(adminPercentage)
                                .creatorPercentage(creatorPercentage)
                                .customerInfo(TransactionDetailDto.CustomerInfo.builder()
                                                .customerId(customer.getCustomerId())
                                                .name(customer.getGivenName() + " " + customer.getFamilyName())
                                                .email(customer.getEmail())
                                                .build())
                                .courseInfo(TransactionDetailDto.CourseInfo.builder()
                                                .courseId(course.getCourseId())
                                                .name(course.getName())
                                                .imageUrl(course.getUrlImg())
                                                .price(course.getPrice())
                                                .build())
                                .creatorInfo(TransactionDetailDto.CreatorInfo.builder()
                                                .creatorId(creator.getCreatorId())
                                                .name(creator.getFullName())
                                                .email(creator.getCustomer() != null ? creator.getCustomer().getEmail()
                                                                : null)
                                                .isFrozen(creator.isBan())
                                                .build())
                                .enrollmentId(enrollment.getEnrollId())
                                .enrollmentDate(enrollment.getCreateDate())
                                .isCompleted(enrollment.isFinish())
                                .build();
        }
}
