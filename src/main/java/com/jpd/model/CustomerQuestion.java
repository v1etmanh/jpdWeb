package com.jpd.model;


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
    private Long id;

    @Column(name = "available__request", nullable = false)
    private Integer availableRequest;


    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to Course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

}
