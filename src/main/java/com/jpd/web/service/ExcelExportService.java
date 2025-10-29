package com.jpd.web.service;

import com.jpd.web.model.CustomerTransaction;
import com.jpd.web.repository.CustomerTransactionRepository;
import com.jpd.web.service.utils.TimeRangeCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelExportService {

    private final CustomerTransactionRepository transactionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export monthly transaction report to Excel
     */
    public byte[] exportMonthlyReport(int month, int year) throws IOException {
        TimeRangeCalculator.TimeRange range = TimeRangeCalculator.getMonthRange(month, year);
        String periodLabel = "Month " + month + ", " + year;

        return createExcelReport(range.getStart(), range.getEnd(), "MONTH", periodLabel);
    }

    /**
     * Export quarterly transaction report to Excel
     */
    public byte[] exportQuarterlyReport(int quarter, int year) throws IOException {
        TimeRangeCalculator.TimeRange range = TimeRangeCalculator.getQuarterRange(quarter, year);
        String periodLabel = "Q" + quarter + " " + year;

        return createExcelReport(range.getStart(), range.getEnd(), "QUARTER", periodLabel);
    }

    /**
     * Export yearly transaction report to Excel
     */
    public byte[] exportYearlyReport(int year) throws IOException {
        TimeRangeCalculator.TimeRange range = TimeRangeCalculator.getYearRange(year);
        String periodLabel = "Year " + year;

        return createExcelReport(range.getStart(), range.getEnd(), "YEAR", periodLabel);
    }

    /**
     * Export transaction list to Excel
     */
    public byte[] exportTransactionList(List<CustomerTransaction> transactions) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transactions");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Transaction ID", "Customer", "Course", "Creator",
                    "Amount", "Currency", "Admin Share", "Creator Share",
                    "Status", "Payment Method", "Created At"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Add data rows
            int rowNum = 1;
            for (CustomerTransaction transaction : transactions) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(transaction.getTransactionID());
                row.createCell(1).setCellValue(
                        transaction.getEnrollment().getCustomer().getGivenName() + " " +
                                transaction.getEnrollment().getCustomer().getFamilyName());
                row.createCell(2).setCellValue(transaction.getEnrollment().getCourse().getName());
                row.createCell(3).setCellValue(transaction.getEnrollment().getCourse().getCreator().getFullName());
                row.createCell(4).setCellValue(transaction.getAmount());
                row.createCell(5).setCellValue(transaction.getCurrency());
                row.createCell(6).setCellValue(transaction.getAdminGet());
                row.createCell(7).setCellValue(transaction.getCreatorGet());
                row.createCell(8).setCellValue(transaction.getStatus());
                row.createCell(9).setCellValue(
                        transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : "N/A");
                row.createCell(10).setCellValue(
                        transaction.getCreatedAt().format(DATE_FORMATTER));

                // Apply style
                for (int i = 0; i < 11; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create comprehensive Excel report with summary and transactions
     */
    private byte[] createExcelReport(LocalDateTime startDate, LocalDateTime endDate,
            String periodType, String periodLabel) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Get summary data
            Double totalRevenue = transactionRepository.getTotalRevenue(startDate, endDate);
            Double adminRevenue = transactionRepository.getAdminRevenue(startDate, endDate);
            Double creatorRevenue = transactionRepository.getCreatorRevenue(startDate, endDate);
            Long totalCount = transactionRepository.countByStatusAndDateRange("SUCCESS", startDate, endDate);

            // Get all transactions in period
            List<CustomerTransaction> transactions = transactionRepository.findAll(
                    (root, query, cb) -> cb.between(root.get("createdAt"), startDate, endDate));

            // Create Summary sheet
            createSummarySheet(workbook, periodLabel, totalRevenue, adminRevenue,
                    creatorRevenue, totalCount, startDate, endDate);

            // Create Transactions sheet
            createTransactionsSheet(workbook, transactions);

            // Create Top Performers sheet
            createTopPerformersSheet(workbook, startDate, endDate);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * Create summary sheet
     */
    private void createSummarySheet(XSSFWorkbook workbook, String periodLabel,
            Double totalRevenue, Double adminRevenue,
            Double creatorRevenue, Long totalCount,
            LocalDateTime startDate, LocalDateTime endDate) {
        Sheet sheet = workbook.createSheet("Summary");

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Transaction Report - " + periodLabel);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        rowNum++; // Skip a row

        // Report Period
        Row periodRow = sheet.createRow(rowNum++);
        periodRow.createCell(0).setCellValue("Report Period:");
        periodRow.getCell(0).setCellStyle(headerStyle);
        periodRow.createCell(1).setCellValue(periodLabel);
        periodRow.getCell(1).setCellStyle(dataStyle);

        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Date Range:");
        dateRow.getCell(0).setCellStyle(headerStyle);
        dateRow.createCell(1).setCellValue(
                startDate.format(DATE_FORMATTER) + " to " + endDate.format(DATE_FORMATTER));
        dateRow.getCell(1).setCellStyle(dataStyle);

        rowNum++;

        // Revenue Summary
        Row revenueHeader = sheet.createRow(rowNum++);
        revenueHeader.createCell(0).setCellValue("Revenue Summary");
        revenueHeader.getCell(0).setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

        Row totalRow = sheet.createRow(rowNum++);
        totalRow.createCell(0).setCellValue("Total Revenue:");
        totalRow.getCell(0).setCellStyle(headerStyle);
        Cell totalCell = totalRow.createCell(1);
        totalCell.setCellValue(totalRevenue != null ? totalRevenue : 0.0);
        totalCell.setCellStyle(dataStyle);

        Row adminRow = sheet.createRow(rowNum++);
        adminRow.createCell(0).setCellValue("Admin Share:");
        adminRow.getCell(0).setCellStyle(headerStyle);
        Cell adminCell = adminRow.createCell(1);
        adminCell.setCellValue(adminRevenue != null ? adminRevenue : 0.0);
        adminCell.setCellStyle(dataStyle);

        Row creatorRow = sheet.createRow(rowNum++);
        creatorRow.createCell(0).setCellValue("Creator Share:");
        creatorRow.getCell(0).setCellStyle(headerStyle);
        Cell creatorCell = creatorRow.createCell(1);
        creatorCell.setCellValue(creatorRevenue != null ? creatorRevenue : 0.0);
        creatorCell.setCellStyle(dataStyle);

        rowNum++;

        // Transaction Count
        Row countRow = sheet.createRow(rowNum++);
        countRow.createCell(0).setCellValue("Total Transactions:");
        countRow.getCell(0).setCellStyle(headerStyle);
        Cell countCell = countRow.createCell(1);
        countCell.setCellValue(totalCount);
        countCell.setCellStyle(dataStyle);

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create transactions sheet with all transaction details
     */
    private void createTransactionsSheet(XSSFWorkbook workbook, List<CustomerTransaction> transactions) {
        Sheet sheet = workbook.createSheet("Transactions");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        // Create header
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Transaction ID", "Customer", "Course", "Creator",
                "Amount", "Currency", "Admin Get", "Creator Get",
                "Status", "Payment Method", "Created At"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data
        int rowNum = 1;
        for (CustomerTransaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(transaction.getTransactionID());
            row.createCell(1).setCellValue(
                    transaction.getEnrollment().getCustomer().getGivenName() + " " +
                            transaction.getEnrollment().getCustomer().getFamilyName());
            row.createCell(2).setCellValue(transaction.getEnrollment().getCourse().getName());
            row.createCell(3).setCellValue(transaction.getEnrollment().getCourse().getCreator().getFullName());

            Cell amountCell = row.createCell(4);
            amountCell.setCellValue(transaction.getAmount());
            amountCell.setCellStyle(dataStyle);

            row.createCell(5).setCellValue(transaction.getCurrency());

            Cell adminCell = row.createCell(6);
            adminCell.setCellValue(transaction.getAdminGet());
            adminCell.setCellStyle(dataStyle);

            Cell creatorCell = row.createCell(7);
            creatorCell.setCellValue(transaction.getCreatorGet());
            creatorCell.setCellStyle(dataStyle);

            row.createCell(8).setCellValue(transaction.getStatus());
            row.createCell(9).setCellValue(
                    transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : "N/A");
            row.createCell(10).setCellValue(
                    transaction.getCreatedAt().format(DATE_FORMATTER));

            // Apply style to all cells
            for (int i = 0; i < 11; i++) {
                if (row.getCell(i) != null) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create top performers sheet
     */
    private void createTopPerformersSheet(XSSFWorkbook workbook, LocalDateTime start, LocalDateTime end) {
        Sheet sheet = workbook.createSheet("Top Performers");

        CellStyle headerStyle = createHeaderStyle(workbook);

        // Top Courses Header
        Row coursesHeader = sheet.createRow(0);
        Cell coursesHeaderCell = coursesHeader.createCell(0);
        coursesHeaderCell.setCellValue("Top 10 Courses by Revenue");
        coursesHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row coursesColHeader = sheet.createRow(1);
        String[] courseHeaders = { "Rank", "Course Name", "Revenue", "Enrollments" };
        for (int i = 0; i < courseHeaders.length; i++) {
            Cell cell = coursesColHeader.createCell(i);
            cell.setCellValue(courseHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // Note: We would need to call the repository method here
        // For now, just create the structure
        // TODO: Implement top performers data population

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
    }

    /**
     * Create header style
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    /**
     * Create title style
     */
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    /**
     * Create data style
     */
    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
