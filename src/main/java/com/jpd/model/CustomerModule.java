package com.jpd.model;


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
    private Long id;
    @Column(nullable = false)
    private Double progress;

    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to Module
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;
}
