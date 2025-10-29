package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

@Entity

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enroll_id")
    private long enrollId;
    @Column(name = "create_date")
    @CreationTimestamp
    private LocalDateTime createDate;

    //link to Course
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference("course-enrollment")
    private Course course;

    //link to Customer
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference("customer-enrollment")  // ← Thêm tên
    private Customer customer;

    //link to feedback
    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("enrollment-feedback")
    private Feedback feedback;

    //link to Transaction
    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerTransaction transaction;
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL)
    @JsonManagedReference("enrollment-customerContent")  // ← ĐỔI thành 
    private List<CustomerModuleContent> customerModuleContents;
    
    private boolean isFinish;
    @PrePersist
    public void setUpFi() {
    	isFinish=false;
    }
}
