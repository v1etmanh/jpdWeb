package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;

@Entity
@Table(
        name = "enrollment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_enrollment_customer_course", columnNames = {"customer_id", "course_id"})
        },
        indexes = {
                @Index(name = "idx_enroll_course", columnList = "course_id"),
                @Index(name = "idx_enroll_customer", columnList = "customer_id")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enroll_id")
    private long id;
    @Column(name = "create_date")
    @CreationTimestamp
    private Date createDate;

    //link to Course
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference
    private Course course;

    //link to Customer
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Customer customer;

    //link to feedback
    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Feedback feedback;

    //link to Transaction
    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    private CustomerTransaction transaction;


}
