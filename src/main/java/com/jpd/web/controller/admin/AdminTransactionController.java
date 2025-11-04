package com.jpd.web.controller.admin;

import com.jpd.web.dto.*;
import com.jpd.web.service.AdminTransactionService;
import com.jpd.web.service.ExcelExportService;
import com.jpd.web.service.utils.TimeRangeCalculator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionController {

    private final AdminTransactionService transactionService;
    private final ExcelExportService excelExportService;

    /**
     * Get transactions with comprehensive filters
     * Supports multi-sort: ?sort=createdAt,desc&sort=amount,desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionListDto>>> getTransactions(
            @Valid @ModelAttribute TransactionFilterDto filter) {
        log.info("Admin fetching transactions with filters: {}", filter);
        Page<TransactionListDto> data = transactionService.getTransactionList(filter);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get transaction detail by ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionDetailDto>> getTransactionDetail(
            @PathVariable Long transactionId) {
        TransactionDetailDto data = transactionService.getTransactionDetail(transactionId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get revenue report for a period
     */
    @GetMapping("/revenue-report")
    public ResponseEntity<ApiResponse<RevenueReportDto>> getRevenueReport(
            @RequestParam String period,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) Integer year) {

        // Calculate time range based on period type
        TimeRangeCalculator.TimeRange range;

        if (startDate != null && endDate != null) {
            range = new TimeRangeCalculator.TimeRange(startDate, endDate);
        } else {
            int currentYear = year != null ? year : LocalDateTime.now().getYear();

            range = switch (period.toUpperCase()) {
                case "MONTH" -> {
                	  
                    int m = month != null ? month : LocalDateTime.now().getMonthValue();
                    yield TimeRangeCalculator.getMonthRange(m, currentYear);
                }
                case "QUARTER" -> {
              
                    int q = quarter != null ? quarter : ((LocalDateTime.now().getMonthValue() - 1) / 3 + 1);
                    yield TimeRangeCalculator.getQuarterRange(q, currentYear);
                }
                case "YEAR" -> {
              
                yield TimeRangeCalculator.getYearRange(currentYear);
                }
                default -> throw new IllegalArgumentException("Invalid period type: " + period);
            };
        }
    
        RevenueReportDto data = transactionService.getRevenueReport(
                period,
                range.getStart(),
                range.getEnd());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Get failed transactions
     */
    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<Page<TransactionListDto>>> getFailedTransactions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TransactionListDto> data = transactionService.getFailedTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Freeze creator revenue (prevent withdrawals)
     */
    @PostMapping("/freeze-creator-revenue")
    public ResponseEntity<ApiResponse<Map<String, String>>> freezeCreatorRevenue(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {

        Long creatorId = Long.valueOf(request.get("creatorId").toString());
        String reason = request.get("reason").toString();
        String adminEmail = jwt.getClaimAsString("email");

        transactionService.freezeCreatorRevenue(creatorId, reason, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Creator revenue frozen successfully");
        response.put("creatorId", creatorId.toString());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Unfreeze creator revenue (allow withdrawals)
     */
    @PostMapping("/unfreeze-creator-revenue")
    public ResponseEntity<ApiResponse<Map<String, String>>> unfreezeCreatorRevenue(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Jwt jwt) {

        Long creatorId = Long.valueOf(request.get("creatorId").toString());
        String adminEmail = jwt.getClaimAsString("email");

        transactionService.unfreezeCreatorRevenue(creatorId, adminEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Creator revenue unfrozen successfully");
        response.put("creatorId", creatorId.toString());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Export monthly report to Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<Resource> exportMonthlyExcel(
            @RequestParam(defaultValue = "1") @Min(1) @Max(12) int month,
            @RequestParam(defaultValue = "2024") int year) throws IOException {

        log.info("Exporting monthly Excel report for month {}, year {}", month, year);

        byte[] excelData = excelExportService.exportMonthlyReport(month, year);

        ByteArrayResource resource = new ByteArrayResource(excelData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                String.format("transactions_month_%d_%d.xlsx", month, year));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(excelData.length)
                .body(resource);
    }

    /**
     * Export quarterly report to Excel
     */
    @GetMapping("/export/excel/quarterly")
    public ResponseEntity<Resource> exportQuarterlyExcel(
            @RequestParam(defaultValue = "1") @Min(1) @Max(4) int quarter,
            @RequestParam(defaultValue = "2024") int year) throws IOException {

        log.info("Exporting quarterly Excel report for quarter {}, year {}", quarter, year);

        byte[] excelData = excelExportService.exportQuarterlyReport(quarter, year);

        ByteArrayResource resource = new ByteArrayResource(excelData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                String.format("transactions_q%d_%d.xlsx", quarter, year));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(excelData.length)
                .body(resource);
    }

    /**
     * Export yearly report to Excel
     */
    @GetMapping("/export/excel/yearly")
    public ResponseEntity<Resource> exportYearlyExcel(
            @RequestParam(defaultValue = "2024") int year) throws IOException {

        log.info("Exporting yearly Excel report for year {}", year);

        byte[] excelData = excelExportService.exportYearlyReport(year);

        ByteArrayResource resource = new ByteArrayResource(excelData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                String.format("transactions_year_%d.xlsx", year));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(excelData.length)
                .body(resource);
    }
}
