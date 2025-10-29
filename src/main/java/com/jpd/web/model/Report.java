package com.jpd.web.model;

import org.springframework.data.convert.ReadingConverter;

import jakarta.annotation.Generated;
<<<<<<< HEAD
import jakarta.persistence.*;
=======
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
>>>>>>> jpdWeb6/master
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
<<<<<<< HEAD
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
=======
>>>>>>> jpdWeb6/master

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Report {
<<<<<<< HEAD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long reportId;
    @Enumerated(EnumType.STRING)
    private ReportType type;
    private String detail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "status")
    private String status = "NEW"; // NEW, REVIEWING, RESOLVED, DISMISSED

    @Column(name = "reviewed_by_admin")
    private String reviewedByAdmin;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
=======
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private long reportId;
@Enumerated(EnumType.STRING)
private ReportType type;
private String detail;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "course_id")
private Course course;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private Customer customer;
>>>>>>> jpdWeb6/master
}
