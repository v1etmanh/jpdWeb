package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    @JsonBackReference
    private Customer customer;

    //link to Chapter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

}
