package com.jpd.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Table(name = "withdraw", indexes = @Index(columnList = "creator_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Withdraw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;
    private String content;
    @Column(name = "create_date")
    @CreationTimestamp
    private LocalDate createDate;


    //link to Creator
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private Creator creator;


}
