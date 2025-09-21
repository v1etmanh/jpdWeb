package com.jpd.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean is_special;
    private String title;


    //link to Module
    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;


}
