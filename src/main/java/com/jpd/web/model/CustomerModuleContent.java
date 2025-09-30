package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "customer_question")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerModuleContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cq_id")
    private long cqId;

    @Column(name = "available__request", nullable = false)
    private int availableRequest;


    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enroll_id")
    private Enrollment enrollment;

    //link to Course
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mc_id")
    private ModuleContent moduleContent;

}
