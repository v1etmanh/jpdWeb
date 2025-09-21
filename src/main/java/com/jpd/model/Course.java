package com.jpd.model;


import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private byte language;
    private double capacity;
    private String description;
    private boolean isBan;
    private boolean isPublic;
    private String learning_object;
    private String name;
    private int number_of_student;
    private BigDecimal price;
    private String requirement;
    private BigDecimal revenue;
    private String target_audience;
    private String url_img;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private Creator creator;


}
