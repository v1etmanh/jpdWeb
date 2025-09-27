package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "progress_course",
        uniqueConstraints = @UniqueConstraint(name = "uq_tracker_customer_course", columnNames = {"customer_id", "course_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    // progress percentage from 0.0 to 100.0
    @Column(nullable = false)
    private double progress;


    // link to Course
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;


    // link to Customer
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Customer customer;
}
