package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "customer_chapter",
        uniqueConstraints = @UniqueConstraint(name = "uq_enrollment_customer_chapter", columnNames = {"customer_id", "chapter_id"})
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private double progress;


    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to Chapter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

}
