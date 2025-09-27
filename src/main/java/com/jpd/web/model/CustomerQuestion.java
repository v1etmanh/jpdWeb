package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "customer_question")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "available__request", nullable = false)
    private int availableRequest;


    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;

    //link to Course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonBackReference
    private Course course;

}
