package com.jpd.web.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadDumpDTO {
    private Integer totalThreads;
    private Integer daemonThreads;
    private Integer peakThreads;
    private List<ThreadInfo> threads;
    private List<String> deadlocks;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThreadInfo {
        private Long threadId;
        private String threadName;
        private String threadState;
        private Long blockedTime;
        private Long blockedCount;
        private Long waitedTime;
        private Long waitedCount;
        private String lockName;
        private List<String> stackTrace;
    }
}