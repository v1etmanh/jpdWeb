package com.jpd.web.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionInfo implements Serializable {
    
    // === Session Identity ===
    private String sessionId;           // UUID unique cho session
    private String sessionCode;         // Mã PIN 6 số để học sinh join
    
    // === Quiz Information ===
    private Long kahootId;              // Link đến KahootListFunction
    private String title;               // Tiêu đề quiz
    private List<Long> questionIds;     // Danh sách mcId của các câu hỏi
    private Integer totalQuestions;     // Tổng số câu hỏi
    
    // === Teacher Information ===
    private Long teacherId;             // ID giáo viên
    private String teacherName;         // Tên giáo viên
    
    // === Session Status ===
    private SessionStatus status;       // WAITING, ACTIVE, PAUSED, FINISHED
    
    // === Current Question State ===
    private Integer currentQuestionIndex;   // Câu hỏi thứ mấy (0-based)
    private Long currentQuestionId;         // mcId của câu hỏi hiện tại
    private TypeOfContent currentQuestionType; // MULTIPLE_CHOICE, GAPFILL
    private LocalDateTime questionStartTime;         // Timestamp bắt đầu câu hỏi
    private Integer questionTimeLimit;      // Giới hạn thời gian (seconds)
    private boolean acceptingAnswers;       // Có đang nhận câu trả lời không
    private LocalDateTime questionEndTime; // Thời gian kết thúc câu hỏi
    // === Session Timing ===
    private LocalDateTime createdAt;    // Thời gian tạo session
    private LocalDateTime startedAt;    // Thời gian bắt đầu quiz
    private LocalDateTime finishedAt;   // Thời gian kết thúc
    
    // === Statistics ===
    private Integer totalParticipants;  // Tổng số người tham gia
    private Integer currentAnswers;     // Số người đã trả lời câu hiện tại
    
    // === Settings ===
    private boolean showLeaderboardAfterEachQuestion; // Hiển thị bảng xếp hạng sau mỗi câu
    private boolean randomizeQuestions; // Xáo trộn câu hỏi
    private boolean randomizeOptions;   // Xáo trộn đáp án
}