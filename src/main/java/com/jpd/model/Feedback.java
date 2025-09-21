package com.jpd.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;
    private double rate;


    //link to Enrollment
    @ManyToOne
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;


}
