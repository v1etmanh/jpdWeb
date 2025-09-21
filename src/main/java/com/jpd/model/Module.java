package com.jpd.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;


    //    link to Course
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;


    //    link to Lesson
    @OneToMany(cascade = CascadeType.ALL)
    private List<Lesson> lessons;


}
