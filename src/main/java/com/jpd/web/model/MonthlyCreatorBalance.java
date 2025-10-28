package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(
    name = "monthly_creator_balance",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"creator_id", "year", "month"}
    ),
    indexes = {
        @Index(name = "idx_creator_year_month", columnList = "creator_id, year, month")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyCreatorBalance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonBackReference("creator-monthlyBalance")
    private Creator creator;
    
    @Column(nullable = false)
    private int year;
    
    @Column(nullable = false)
    private int month; // 1-12
    
    private double totalRevenue;
    private long totalStudents;
    private long totalCourses;
    private double avgRating;
   
    private double completionRate;
    private long newEnrollments;
    private long totalReviews;
    @ElementCollection
    @CollectionTable(
        name = "list_popular_courses",
        joinColumns = @JoinColumn(name = "balance_id")
    )
    private List<Long> popularCourses;
    @Column(name = "last_updated")
    private java.time.LocalDateTime lastUpdated;
    
    // Helper method
    public String getYearMonth() {
        return String.format("%d-%02d", year, month);
    }
}