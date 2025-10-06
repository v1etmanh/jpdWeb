package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;


@Entity

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   
   
    @Column(name = "customer_id")
    private long customerId;
    @Column(name = "create_date")
    @CreationTimestamp
    private Date createDate;
    private String email;
    @Column(name = "family_name")
    private String familyName;
    @Column(name = "given_name")
    private String givenName;
    private String role;
    private String username;

    //link to Creator
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Creator creator;

    //link to Remember_word
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    
    private List<Remember_word> rememberWords;



    //link to Wish_list
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    
    private List<Wishlist> wishlists;

    //link to Enrollment
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference("customer-enrollment")
    private List<Enrollment> enrollments;



}
