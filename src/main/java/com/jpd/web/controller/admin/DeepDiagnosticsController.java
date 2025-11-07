package com.jpd.web.controller.admin;

import org.springframework.boot.logging.LogLevel;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeansDescriptor;
import org.springframework.boot.actuate.beans.BeansEndpoint.ContextBeansDescriptor;
import org.springframework.boot.actuate.beans.BeansEndpoint.BeanDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentEntryDescriptor;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertyValueDescriptor;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.actuate.management.ThreadDumpEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.actuate.logging.LoggersEndpoint.LoggerLevelsDescriptor;

import com.jpd.web.dto.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/monitoring/diagnostics")
@RequiredArgsConstructor

public class DeepDiagnosticsController {

    private final ThreadDumpEndpoint threadDumpEndpoint;
    private final LoggersEndpoint loggersEndpoint;
    private final EnvironmentEndpoint environmentEndpoint;
    private final BeansEndpoint beansEndpoint;

    // ==================== THREAD DUMP ====================

    @GetMapping("/threads")
    public ResponseEntity<ThreadDumpDTO> getThreadDump() {
        try {
            ThreadDumpEndpoint.ThreadDumpDescriptor dump = threadDumpEndpoint.threadDump();

            List<ThreadDumpDTO.ThreadInfo> threads = dump.getThreads().stream()
                    .map(thread -> {
                        List<String> stackTraceList = new ArrayList<>();
                        if (thread.getStackTrace() != null) {
                            stackTraceList = Arrays.stream(thread.getStackTrace())
                                    .limit(10)
                                    .map(StackTraceElement::toString)
                                    .collect(Collectors.toList());
                        }

                        return ThreadDumpDTO.ThreadInfo.builder()
                                .threadId(thread.getThreadId())
                                .threadName(thread.getThreadName())
                                .threadState(thread.getThreadState().toString())
                                .blockedTime(thread.getBlockedTime())
                                .blockedCount(thread.getBlockedCount())
                                .waitedTime(thread.getWaitedTime())
                                .waitedCount(thread.getWaitedCount())
                                .lockName(thread.getLockName())
                                .stackTrace(stackTraceList)
                                .build();
                    })
                    .collect(Collectors.toList());

            ThreadDumpDTO result = ThreadDumpDTO.builder()
                    .totalThreads(threads.size())
                    .daemonThreads((int) threads.stream()
                            .filter(t -> t.getThreadName().contains("daemon"))
                            .count())
                    .peakThreads(threads.size())
                    .threads(threads)
                    .deadlocks(new ArrayList<>())
                    .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error getting thread dump", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/threads/summary")
    public ResponseEntity<Map<String, Object>> getThreadSummary() {
        try {
            ThreadDumpEndpoint.ThreadDumpDescriptor dump = threadDumpEndpoint.threadDump();

            Map<String, Long> stateCount = dump.getThreads().stream()
                    .collect(Collectors.groupingBy(
                            thread -> thread.getThreadState().toString(),
                            Collectors.counting()
                    ));

            return ResponseEntity.ok(Map.of(
                    "total", dump.getThreads().size(),
                    "states", stateCount
            ));

        } catch (Exception e) {
            log.error("Error getting thread summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== LOGGERS ====================

    @GetMapping("/loggers")
    public ResponseEntity<LoggerListDTO> getLoggers(
            @RequestParam(required = false) String filter
    ) {
        try {
            // ✅ Lấy descriptor gốc
            LoggersEndpoint.LoggersDescriptor descriptor = loggersEndpoint.loggers();

            // ✅ Lấy Map loggers từ descriptor
            Map<String, LoggerLevelsDescriptor> loggersMap = descriptor.getLoggers();

            List<LoggerDTO> loggerList = loggersMap.entrySet().stream()
                    .filter(entry -> filter == null ||
                            entry.getKey().toLowerCase().contains(filter.toLowerCase()))
                    .map(entry -> {
                        LoggerLevelsDescriptor levels = entry.getValue();

                        String configuredLevel = levels.getConfiguredLevel() != null
                                ? levels.getConfiguredLevel().toString()
                                : null;

                        String effectiveLevel = levels.getConfiguredLevel() != null
                                ? levels.getConfiguredLevel().toString()
                                : null;

                        return LoggerDTO.builder()
                                .name(entry.getKey())
                                .configuredLevel(configuredLevel)
                                .effectiveLevel(effectiveLevel)
                                .build();
                    })
                    .sorted(Comparator.comparing(LoggerDTO::getName))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(LoggerListDTO.builder()
                    .loggers(loggerList)
                    .total(loggerList.size())
                    .build());

        } catch (Exception e) {
            log.error("Error getting loggers", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/loggers/{name:.+}")
    public ResponseEntity<LoggerDTO> getLogger(@PathVariable String name) {
        try {
        	LoggerLevelsDescriptor levels = loggersEndpoint.loggerLevels(name);

            if (levels == null) {
                return ResponseEntity.notFound().build();
            }

            String configuredLevel = null;
            if (levels.getConfiguredLevel() != null) {
                configuredLevel = levels.getConfiguredLevel().toString();
            }
            
            String effectiveLevel = null;
            if (levels.getConfiguredLevel() != null) {
                effectiveLevel = levels.getConfiguredLevel().toString();
            }

            return ResponseEntity.ok(LoggerDTO.builder()
                    .name(name)
                    .configuredLevel(configuredLevel)
                    .effectiveLevel(effectiveLevel)
                    .build());

        } catch (Exception e) {
            log.error("Error getting logger: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/loggers/{name:.+}")
    public ResponseEntity<Map<String, String>> updateLogLevel(
            @PathVariable String name,
            @RequestBody Map<String, String> request
    ) {
        try {
            String levelStr = request.get("level");
            if (levelStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Level is required"));
            }

            LogLevel logLevel = LogLevel.valueOf(levelStr.toUpperCase());
            loggersEndpoint.configureLogLevel(name, logLevel);

            log.info("Changed log level for {} to {}", name, logLevel);

            return ResponseEntity.ok(Map.of(
                    "message", "Log level updated successfully",
                    "logger", name,
                    "level", logLevel.toString()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid log level"));
        } catch (Exception e) {
            log.error("Error updating log level", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== ENVIRONMENT ====================

    @GetMapping("/environment")
    public ResponseEntity<EnvironmentDTO> getEnvironment(
            @RequestParam(required = false) String filter
    ) {
        try {
            EnvironmentDescriptor envDescriptor = environmentEndpoint.environment(null);

            List<EnvironmentDTO.PropertySource> propertySources = envDescriptor.getPropertySources().stream()
                    .map(ps -> {
                        Map<String, Object> properties = new HashMap<>();

                        ps.getProperties().forEach((key, valueDescriptor) -> {
                            if (filter == null || key.toLowerCase().contains(filter.toLowerCase())) {
                                Object value = valueDescriptor.getValue();

                                // Mask sensitive values
                                String keyLower = key.toLowerCase();
                                if (keyLower.contains("password") || keyLower.contains("secret") || keyLower.contains("token")) {
                                    value = "******";
                                }

                                properties.put(key, value);
                            }
                        });

                        return EnvironmentDTO.PropertySource.builder()
                                .name(ps.getName())
                                .properties(properties)
                                .build();
                    })
                    .filter(ps -> !ps.getProperties().isEmpty())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(EnvironmentDTO.builder()
                    .propertySources(propertySources)
                    .build());

        } catch (Exception e) {
            log.error("Error getting environment", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/environment/{property:.+}")
    public ResponseEntity<Map<String, Object>> getEnvironmentProperty(
            @PathVariable String property
    ) {
        try {
            EnvironmentEntryDescriptor entryDescriptor = 
                    environmentEndpoint.environmentEntry(property);

            if (entryDescriptor == null) {
                return ResponseEntity.notFound().build();
            }

            List<Map<String, Object>> propertySources = entryDescriptor.getPropertySources().stream()
                    .map(ps -> {
                        Map<String, Object> sourceMap = new HashMap<>();
                        sourceMap.put("name", ps.getName());
                        
                        Map<String, Object> propertyMap = new HashMap<>();
                        propertyMap.put("value", ps.getProperty().getValue());
                        propertyMap.put("origin", ps.getProperty().getOrigin());
                        
                        sourceMap.put("property", propertyMap);
                        return sourceMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "property", property,
                    "propertySources", propertySources
            ));

        } catch (Exception e) {
            log.error("Error getting environment property: {}", property, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== BEANS ====================

    @GetMapping("/beans")
    public ResponseEntity<BeanListDTO> getBeans(
            @RequestParam(required = false) String filter
    ) {
        try {
            BeansDescriptor beansDescriptor = beansEndpoint.beans();

            List<BeanDTO> beanList = new ArrayList<>();
            String normalizedFilter = (filter == null) ? null : filter.trim().toLowerCase();

            beansDescriptor.getContexts().forEach((contextName, contextBeansDescriptor) -> {
                contextBeansDescriptor.getBeans().forEach((beanName, beanDescriptor) -> {
                    // ✅ getType() trả về Class<?>, cần chuyển sang String
                    String typeName = beanDescriptor.getType() != null ? 
                            beanDescriptor.getType().getName() : "";

                    boolean matches = false;
                    if (normalizedFilter == null || normalizedFilter.isEmpty()) {
                        matches = true;
                    } else {
                        String beanNameLower = beanName == null ? "" : beanName.toLowerCase();
                        String typeNameLower = typeName.toLowerCase();

                        if (beanNameLower.contains(normalizedFilter) || typeNameLower.contains(normalizedFilter)) {
                            matches = true;
                        }
                    }

                    if (matches) {
                        beanList.add(BeanDTO.builder()
                                .name(beanName)
                                .type(typeName)
                                .scope(beanDescriptor.getScope())
                                .resource(beanDescriptor.getResource())
                                .build());
                    }
                });
            });

            beanList.sort(Comparator.comparing(BeanDTO::getName, Comparator.nullsFirst(String::compareToIgnoreCase)));

            return ResponseEntity.ok(BeanListDTO.builder()
                    .beans(beanList)
                    .total(beanList.size())
                    .build());

        } catch (Exception e) {
            log.error("Error getting beans", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/beans/summary")
    public ResponseEntity<Map<String, Object>> getBeansSummary() {
        try {
            BeansDescriptor beansDescriptor = beansEndpoint.beans();

            Map<String, Long> packageCount = new HashMap<>();
            int totalBeans = 0;

            for (Map.Entry<String, ContextBeansDescriptor> contextEntry : beansDescriptor.getContexts().entrySet()) {
                for (Map.Entry<String, BeanDescriptor> beanEntry : contextEntry.getValue().getBeans().entrySet()) {
                    totalBeans++;

                    // ✅ getType() trả về Class<?>, cần chuyển sang String
                    Class<?> typeClass = beanEntry.getValue().getType();
                    if (typeClass != null) {
                        String type = typeClass.getName();
                        if (type.contains(".")) {
                            String packageName = type.substring(0, type.lastIndexOf('.'));
                            packageCount.merge(packageName, 1L, Long::sum);
                        }
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "total", totalBeans,
                    "packages", packageCount
            ));

        } catch (Exception e) {
            log.error("Error getting beans summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}