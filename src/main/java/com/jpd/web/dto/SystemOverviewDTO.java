package com.jpd.web.dto;

//SystemOverviewDTO.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewDTO {
 private LocalDateTime timestamp;
 private String status;
 private SystemMetrics system;
 private DatabaseMetrics database;
 private HttpMetrics http;
 private ApplicationInfo application;
 
 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public static class SystemMetrics {
     private Double cpuUsage;          // %
     private MemoryInfo memory;
     private DiskInfo disk;
     private Integer activeThreads;
     private Integer peakThreads;
 }
 
 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public static class MemoryInfo {
     private Long used;                // MB
     private Long max;                 // MB
     private Long committed;           // MB
     private Double usagePercent;      // %
 }
 
 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public static class DiskInfo {
     private Long free;                // GB
     private Long total;               // GB
     private Double usagePercent;      // %
 }
 
 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public static class DatabaseMetrics {
     private String status;
     private Integer activeConnections;
     private Integer idleConnections;
     private Integer maxConnections;
     private Integer minConnections;
     private Long totalConnections;
 }
 
 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public static class HttpMetrics {
     private Long totalRequests;
     private Double requestsPerSecond;
     private Double avgResponseTime;    // ms
     private Double errorRate;          // %
 }
 
 @Data
 @Builder
 @NoArgsConstructor
 @AllArgsConstructor
 public static class ApplicationInfo {
     private String name;
     private String version;
     private String buildTime;
     private Long uptime;               // seconds
 }
}