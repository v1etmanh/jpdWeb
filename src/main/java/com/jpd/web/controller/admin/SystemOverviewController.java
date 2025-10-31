package com.jpd.web.controller.admin;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricDescriptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.jpd.web.dto.*;
import com.jpd.web.dto.SystemOverviewDTO.*;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor

public class SystemOverviewController {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;
    private final InfoEndpoint infoEndpoint;
    private final MeterRegistry meterRegistry;

    @GetMapping("/overview")
    public ResponseEntity<SystemOverviewDTO> getSystemOverview() {
        try {
            SystemOverviewDTO overview = SystemOverviewDTO.builder()
                    .timestamp(LocalDateTime.now())
                    .status(getHealthStatus())
                    .system(getSystemMetrics())
                    .database(getDatabaseMetrics())
                    .http(getHttpMetrics())
                    .application(getApplicationInfo())
                    .build();

            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            log.error("Error getting system overview", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try {
            Object healthObj = healthEndpoint.health();
            
            if (healthObj instanceof Health) {
                Health health = (Health) healthObj;
                return ResponseEntity.ok(Map.of(
                        "status", health.getStatus().getCode(),
                        "components", health.getDetails()
                ));
            } else if (healthObj instanceof HealthComponent) {
                HealthComponent health = (HealthComponent) healthObj;
                return ResponseEntity.ok(Map.of(
                        "status", health.getStatus().getCode()
                ));
            }
            
            return ResponseEntity.ok(Map.of("status", "UNKNOWN"));
        } catch (Exception e) {
            log.error("Error getting health", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== Private Helper Methods ====================

    private String getHealthStatus() {
        try {
            Object healthObj = healthEndpoint.health();
            if (healthObj instanceof Health) {
                return ((Health) healthObj).getStatus().getCode();
            } else if (healthObj instanceof HealthComponent) {
                return ((HealthComponent) healthObj).getStatus().getCode();
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.error("Error getting health status", e);
            return "UNKNOWN";
        }
    }

    private SystemMetrics getSystemMetrics() {
        try {
            // CPU Usage
            Double cpuUsage = getMetricValue("system.cpu.usage") * 100;

            // Memory
            Long memoryUsed = getMetricValue("jvm.memory.used").longValue() / (1024 * 1024);
            Long memoryMax = getMetricValue("jvm.memory.max").longValue() / (1024 * 1024);
            Long memoryCommitted = getMetricValue("jvm.memory.committed").longValue() / (1024 * 1024);
            Double memoryUsagePercent = memoryMax > 0 ? (memoryUsed * 100.0 / memoryMax) : 0.0;

            MemoryInfo memory = MemoryInfo.builder()
                    .used(memoryUsed)
                    .max(memoryMax)
                    .committed(memoryCommitted)
                    .usagePercent(Math.round(memoryUsagePercent * 100.0) / 100.0)
                    .build();

            // Disk
            File root = new File("/");
            Long diskFree = root.getFreeSpace() / (1024 * 1024 * 1024);
            Long diskTotal = root.getTotalSpace() / (1024 * 1024 * 1024);
            Double diskUsagePercent = diskTotal > 0 ? ((diskTotal - diskFree) * 100.0 / diskTotal) : 0.0;

            DiskInfo disk = DiskInfo.builder()
                    .free(diskFree)
                    .total(diskTotal)
                    .usagePercent(Math.round(diskUsagePercent * 100.0) / 100.0)
                    .build();

            // Threads
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            Integer activeThreads = threadBean.getThreadCount();
            Integer peakThreads = threadBean.getPeakThreadCount();

            return SystemMetrics.builder()
                    .cpuUsage(Math.round(cpuUsage * 100.0) / 100.0)
                    .memory(memory)
                    .disk(disk)
                    .activeThreads(activeThreads)
                    .peakThreads(peakThreads)
                    .build();

        } catch (Exception e) {
            log.error("Error getting system metrics", e);
            return SystemMetrics.builder().build();
        }
    }

    private DatabaseMetrics getDatabaseMetrics() {
        try {
            // Kiểm tra HikariCP metrics có available không
            if (!isMetricAvailable("hikaricp.connections.active")) {
                log.warn("HikariCP metrics not available, returning status only");
                return DatabaseMetrics.builder()
                        .status(getDbHealthStatus())
                        .build();
            }

            Integer active = getMetricValue("hikaricp.connections.active").intValue();
            Integer idle = getMetricValue("hikaricp.connections.idle").intValue();
            Integer max = getMetricValue("hikaricp.connections.max").intValue();
            Integer min = getMetricValue("hikaricp.connections.min").intValue();
            Long total = getMetricValue("hikaricp.connections").longValue();

            return DatabaseMetrics.builder()
                    .status(getDbHealthStatus())
                    .activeConnections(active)
                    .idleConnections(idle)
                    .maxConnections(max)
                    .minConnections(min)
                    .totalConnections(total)
                    .build();

        } catch (Exception e) {
            log.error("Error getting database metrics", e);
            return DatabaseMetrics.builder()
                    .status("UNKNOWN")
                    .build();
        }
    }

    private String getDbHealthStatus() {
        try {
            Object healthObj = healthEndpoint.health();
            
            if (healthObj instanceof Health) {
                Health health = (Health) healthObj;
                if (health.getDetails().containsKey("db")) {
                    Object dbHealth = health.getDetails().get("db");
                    if (dbHealth instanceof Map) {
                        Object status = ((Map<String, Object>) dbHealth).get("status");
                        return status != null ? status.toString() : "UNKNOWN";
                    }
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.warn("Cannot get DB health status", e);
            return "UNKNOWN";
        }
    }

    private HttpMetrics getHttpMetrics() {
        try {
            Timer timer = meterRegistry.find("http.server.requests").timer();
            
            if (timer == null) {
                log.warn("HTTP metrics timer not found");
                return HttpMetrics.builder().build();
            }

            long totalRequests = timer.count();
            
            // Avg response time
            double avgResponseTime = timer.mean(TimeUnit.MILLISECONDS);

            // Requests per second - tính dựa trên uptime
            long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
            double requestsPerSecond = uptimeSeconds > 0 ? (double) totalRequests / uptimeSeconds : 0.0;

            // Error count - tổng tất cả status 5xx
            long errorCount = meterRegistry.find("http.server.requests")
                    .tag("status", "500")
                    .timers()
                    .stream()
                    .mapToLong(Timer::count)
                    .sum();
            
            // Thêm các status code khác nếu cần
            errorCount += meterRegistry.find("http.server.requests")
                    .tag("status", "503")
                    .timers()
                    .stream()
                    .mapToLong(Timer::count)
                    .sum();

            double errorRate = totalRequests > 0 ? (errorCount * 100.0 / totalRequests) : 0.0;

            return HttpMetrics.builder()
                    .totalRequests(totalRequests)
                    .requestsPerSecond(Math.round(requestsPerSecond * 100.0) / 100.0)
                    .avgResponseTime(Math.round(avgResponseTime * 100.0) / 100.0)
                    .errorRate(Math.round(errorRate * 100.0) / 100.0)
                    .build();

        } catch (Exception e) {
            log.error("Error getting HTTP metrics", e);
            return HttpMetrics.builder().build();
        }
    }

    private ApplicationInfo getApplicationInfo() {
        try {
            Map<String, Object> info = infoEndpoint.info();
            Map<String, Object> appInfo = (Map<String, Object>) info.getOrDefault("app", Map.of());

            Long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

            return ApplicationInfo.builder()
                    .name(appInfo.getOrDefault("name", "Unknown").toString())
                    .version(appInfo.getOrDefault("version", "Unknown").toString())
                    .buildTime(appInfo.getOrDefault("buildTime", "Unknown").toString())
                    .uptime(uptime)
                    .build();

        } catch (Exception e) {
            log.error("Error getting application info", e);
            return ApplicationInfo.builder().build();
        }
    }

    private Double getMetricValue(String metricName) {
        try {
            MetricDescriptor metric = metricsEndpoint.metric(metricName, null);
            if (metric != null && metric.getMeasurements() != null && !metric.getMeasurements().isEmpty()) {
                return metric.getMeasurements().get(0).getValue();
            }
        } catch (Exception e) {
            log.warn("Metric {} not found", metricName);
        }
        return 0.0;
    }

    private boolean isMetricAvailable(String metricName) {
        return meterRegistry.find(metricName).meter() != null;
    }
}