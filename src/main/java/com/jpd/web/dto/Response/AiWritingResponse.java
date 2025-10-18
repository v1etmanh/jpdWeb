package com.jpd.web.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiWritingResponse {

    private Score score;
    private String overallFeedback;
    private List<Tip> tips;

    // --------- Nested DTOs ----------
    @Data
    public static class Score {
        private double overall;
        private int scale;
        private List<Criterion> criteria;
    }
    @Data
    public static class Criterion {
        private String key;   // vd: "task_response"
        private String label; // vd: "Task Response"
        private double score; // điểm từng tiêu chí
        private int max;      // 9

    }
    @Data
    public static class Tip {
        private String title;
        private String detail;

    }
}
