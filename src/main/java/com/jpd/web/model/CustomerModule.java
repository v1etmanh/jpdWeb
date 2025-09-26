package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Data
@Table(name = "customer_module")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private double progress;

    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to Module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;
}
