<!-- c515a466-cbce-4b91-b354-266a61fdd5fc 184c1f9f-9c3e-430f-9352-c15153818226 -->
# Transaction Management Module

## Overview

Implement comprehensive transaction management for admin to track, analyze, and export financial data including enrollment transactions, revenue sharing, and financial reports.

## Current System Analysis

**Existing Models:**

- `CustomerTransaction`: Has amount, creatorGet, adminGet, status, enrollment, paymentId, createdAt/updatedAt
- `PaymentTracking`: PayPal order tracking with status, amount, currency
- `Enrollment`: Links customer, course, and transaction (one-to-one)
- Commission rate: 80% creator, 20% admin (from PayPalService)

**Issues Found:**

- `CustomerTransaction.createdAt/updatedAt` use `java.sql.Date` (should be `LocalDateTime`)
- No payment method field (currently only PayPal)
- Missing indexes for queries
- No frozen balance mechanism for creator violations

## Implementation Plan

### 1. Database Schema Enhancement

**Update CustomerTransaction** (`src/main/java/com/jpd/web/model/CustomerTransaction.java`)

- Change `createdAt` and `updatedAt` from `Date` to `LocalDateTime`
- Add `@CreationTimestamp` and `@UpdateTimestamp` annotations
- Add `paymentMethod` field (PAYPAL, CREDIT_CARD, etc.)
- Add indexes: `@Index(columnList = "status")`, `@Index(columnList = "createdAt")`
- Add computed field helper: `getTotalRevenue()` method

**Update Creator** (already has fields from previous module)

- Use existing: `isBanned`, `status` for frozen balance logic

### 2. Repository Layer

**Enhance CustomerTransactionRepository** (`src/main/java/com/jpd/web/repository/CustomerTransactionRepository.java`)

Add queries:

```java
// Filter queries
List<CustomerTransaction> findByStatus(String status);
Page<CustomerTransaction> findAll(Pageable pageable);

// Date range queries
@Query("SELECT ct FROM CustomerTransaction ct WHERE ct.createdAt BETWEEN :startDate AND :endDate")
List<CustomerTransaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

// Customer/Course/Creator specific
@Query("SELECT ct FROM CustomerTransaction ct WHERE ct.enrollment.customer.customerId = :customerId")
List<CustomerTransaction> findByCustomerId(Long customerId);

@Query("SELECT ct FROM CustomerTransaction ct WHERE ct.enrollment.course.courseId = :courseId")
List<CustomerTransaction> findByCourseId(Long courseId);

@Query("SELECT ct FROM CustomerTransaction ct WHERE ct.enrollment.course.creator.creatorId = :creatorId")
List<CustomerTransaction> findByCreatorId(Long creatorId);

// Revenue statistics
@Query("SELECT SUM(ct.amount) FROM CustomerTransaction ct WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
Double getTotalRevenue(LocalDateTime start, LocalDateTime end);

@Query("SELECT SUM(ct.adminGet) FROM CustomerTransaction ct WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
Double getAdminRevenue(LocalDateTime start, LocalDateTime end);

@Query("SELECT SUM(ct.creatorGet) FROM CustomerTransaction ct WHERE ct.status = 'SUCCESS' AND ct.createdAt BETWEEN :start AND :end")
Double getCreatorRevenue(LocalDateTime start, LocalDateTime end);

// Top courses/creators
@Query("SELECT ct.enrollment.course, SUM(ct.amount) as revenue FROM CustomerTransaction ct " +
       "WHERE ct.status = 'SUCCESS' GROUP BY ct.enrollment.course ORDER BY revenue DESC")
List<Object[]> getTopCoursesByRevenue(Pageable pageable);

@Query("SELECT ct.enrollment.course.creator, SUM(ct.creatorGet) as revenue FROM CustomerTransaction ct " +
       "WHERE ct.status = 'SUCCESS' GROUP BY ct.enrollment.course.creator ORDER BY revenue DESC")
List<Object[]> getTopCreatorsByRevenue(Pageable pageable);

// Failed transactions
List<CustomerTransaction> findByStatusOrderByCreatedAtDesc(String status);
```

### 3. DTO Layer

**Create TransactionListDto** (`src/main/java/com/jpd/web/dto/TransactionListDto.java`)

```java
{
  transactionId, amount, currency, status,
  customerName, customerEmail,
  courseName, courseId,
  creatorName, creatorId,
  adminGet, creatorGet,
  paymentMethod, paymentId,
  createdAt, updatedAt
}
```

**Create TransactionDetailDto** (`src/main/java/com/jpd/web/dto/TransactionDetailDto.java`)

```java
{
  // Basic transaction info
  transactionId, amount, currency, status, content,
  paymentMethod, paymentId,
  createdAt, updatedAt,
  
  // Revenue split
  adminGet, creatorGet, adminPercentage, creatorPercentage,
  
  // Customer info
  customerDto { customerId, name, email },
  
  // Course info
  courseDto { courseId, name, imageUrl, price },
  
  // Creator info
  creatorDto { creatorId, name, email, isFrozen },
  
  // Enrollment info
  enrollmentId, enrollmentDate, isCompleted
}
```

**Create RevenueReportDto** (`src/main/java/com/jpd/web/dto/RevenueReportDto.java`)

```java
{
  periodType (DAY, WEEK, MONTH, QUARTER, YEAR),
  startDate, endDate,
  totalRevenue, adminRevenue, creatorRevenue,
  totalTransactions, successfulTransactions, failedTransactions,
  averageTransactionValue,
  topCourses (List<CourseRevenueDto>),
  topCreators (List<CreatorRevenueDto>)
}
```

**Create TransactionFilterDto** (`src/main/java/com/jpd/web/dto/TransactionFilterDto.java`)

```java
{
  status, customerId, courseId, creatorId,
  startDate, endDate,
  minAmount, maxAmount,
  paymentMethod,
  page, size, sortBy, sortDirection
}
```

### 4. Service Layer

**Create AdminTransactionService** (`src/main/java/com/jpd/web/service/AdminTransactionService.java`)

Methods:

- `getTransactionList(TransactionFilterDto filter)` → Page<TransactionListDto>
- `getTransactionDetail(Long transactionId)` → TransactionDetailDto
- `getRevenueReport(PeriodType period, LocalDateTime start, LocalDateTime end)` → RevenueReportDto
- `getFailedTransactions(Pageable pageable)` → Page<TransactionListDto>
- `freezeCreatorRevenue(Long creatorId, String reason, String adminEmail)` → void
- `unfreezeCreatorRevenue(Long creatorId, String adminEmail)` → void

**Create ExcelExportService** (`src/main/java/com/jpd/web/service/ExcelExportService.java`)

Use Apache POI library for Excel generation:

- `exportMonthlyReport(int month, int year)` → byte[]
- `exportQuarterlyReport(int quarter, int year)` → byte[]
- `exportYearlyReport(int year)` → byte[]
- `exportTransactionList(List<CustomerTransaction> transactions)` → byte[]

Excel structure:

```
Sheet: Summary
- Period, Total Revenue, Admin Revenue, Creator Revenue
- Total Transactions, Success Rate
- Top 10 Courses, Top 10 Creators

Sheet: Transactions
- All transaction details in table format
```

### 5. Controller Layer

**Create AdminTransactionController** (`src/main/java/com/jpd/web/controller/admin/AdminTransactionController.java`)

Endpoints:

```
GET  /api/admin/transactions
     ?status=SUCCESS&customerId=1&courseId=5&creatorId=3
     &startDate=2024-01-01&endDate=2024-12-31
     &page=0&size=20&sortBy=createdAt&sortDirection=DESC
     → List transactions with comprehensive filters

GET  /api/admin/transactions/{transactionId}
     → Get detailed transaction info

GET  /api/admin/transactions/revenue-report
     ?period=MONTH&startDate=2024-01-01&endDate=2024-01-31
     → Get revenue statistics and analysis

GET  /api/admin/transactions/failed
     ?page=0&size=20
     → List failed transactions

GET  /api/admin/transactions/export/excel
     ?period=MONTH&month=1&year=2024
     → Download Excel report (returns file)

GET  /api/admin/transactions/export/excel/quarterly
     ?quarter=1&year=2024
     → Download quarterly Excel report

GET  /api/admin/transactions/export/excel/yearly
     ?year=2024
     → Download yearly Excel report

POST /api/admin/transactions/freeze-creator-revenue
     Body: { creatorId, reason }
     → Freeze revenue for violating creator (BR-19, BR-25)

POST /api/admin/transactions/unfreeze-creator-revenue
     Body: { creatorId }
     → Unfreeze creator revenue
```

### 6. Transform/Mapper Layer

**Create AdminTransactionTransform** (`src/main/java/com/jpd/web/transform/AdminTransactionTransform.java`)

- `toTransactionListDto(CustomerTransaction)`
- `toTransactionDetailDto(CustomerTransaction)`
- `toRevenueReportDto(List<CustomerTransaction>, PeriodType)`

### 7. Excel Export Configuration

**Add Apache POI dependency** to `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

### 8. Freeze Revenue Logic (BR-19, BR-25)

**Implementation in AdminTransactionService:**

- When creator is banned/suspended/under investigation:
  - Set `creator.isBanned = true` or `status = SUSPENDED/UNDER_REVIEW`
  - This prevents withdrawals (already checked in `CreatorService.isValidToWithdraw()`)
  - No need to modify transactions - just prevent withdrawal

- When unfrozen:
  - Restore creator status
  - Log action in AuditLog

### 9. Period Calculation Helpers

**Create TimeRangeCalculator** utility class:

- `getDayRange(LocalDate date)` → (start, end)
- `getWeekRange(LocalDate date)` → (start, end)
- `getMonthRange(int month, int year)` → (start, end)
- `getQuarterRange(int quarter, int year)` → (start, end)
- `getYearRange(int year)` → (start, end)

## Files to Create (8 new)

1. `dto/TransactionListDto.java`
2. `dto/TransactionDetailDto.java`
3. `dto/RevenueReportDto.java`
4. `dto/TransactionFilterDto.java`
5. `service/AdminTransactionService.java`
6. `service/ExcelExportService.java`
7. `controller/admin/AdminTransactionController.java`
8. `transform/AdminTransactionTransform.java`
9. `util/TimeRangeCalculator.java` (utility)

## Files to Modify (2 existing)

1. `model/CustomerTransaction.java` - fix date types, add indexes
2. `repository/CustomerTransactionRepository.java` - add queries
3. `pom.xml` - add Apache POI dependency

## Testing Scenarios

- Filter transactions by date range (daily, monthly, quarterly, yearly)
- Filter by customer, course, creator
- Filter by status (SUCCESS, FAILED)
- Generate revenue reports with correct calculations
- Export Excel with proper formatting
- Freeze/unfreeze creator revenue
- Verify frozen creators cannot withdraw

## Notes

- Revenue freeze works through existing `Creator.isBanned` and `status` fields
- Use `@CreationTimestamp` and `@UpdateTimestamp` for automatic date handling
- Excel export should be streamed for large datasets
- Consider caching for frequently accessed reports
- All monetary calculations should use proper rounding

### To-dos

- [ ] Expand Status enum, create AuditLog & CreatorWarning entities, update Creator and Report models
- [ ] Create AuditLogRepository, CreatorWarningRepository, enhance CreatorRepository and ReportRepository with admin queries
- [ ] Create admin DTOs: AdminCreatorListDto, AdminCreatorDetailDto, CertificateApprovalDto, AdminActionRequest
- [ ] Create AdminCreatorService and AuditLogService with full business logic for certificate approval, warnings, bans
- [ ] Create AdminCreatorTransform for entity-to-DTO mappings with statistics aggregation
- [ ] Create AdminCreatorController with all REST endpoints for creator management
- [ ] Update JaenConfig for /api/admin/** authorization and create AdminFilter