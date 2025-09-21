package com.jpd.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime enroll_date;
    private double progress;

    //link to Course
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    //link to Customer
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to feedback
    @OneToMany
    private List<Feedback> feedbacks;


    //link to customer_finished_lesson


}
