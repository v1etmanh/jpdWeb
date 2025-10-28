package com.jpd.web.model;

public enum SessionStatus {
    WAITING,        // Chờ học sinh join
    ACTIVE,         // Đang chơi
    PAUSED,         // Tạm dừng
    SHOWING_RESULTS, // Đang hiển thị kết quả câu hỏi
    FINISHED        // Kết thúc
}