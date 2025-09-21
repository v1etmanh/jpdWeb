package com.jpd.model;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "creator")
@Data //Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor

public class Creator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;
    private BigDecimal balance;
    private double current_capacity;
    private double max_capacity;
    private String email;
    private String full_name;
    private String image_url;
    private String phone_number;
    private String bank_account_name;
    private String bank_account_number;
    private String description;

    //link to Customer
    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to Course
    @OneToMany
    private List<Course> courses;


}
