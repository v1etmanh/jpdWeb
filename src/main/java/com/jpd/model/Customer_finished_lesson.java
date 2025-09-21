package com.jpd.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Customer_finished_lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    //link to Enrollment
    @ManyToOne
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;


    //link to Lesson
//    private





}
