package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "creator")
@Data //Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private long id;
    @Column(nullable = false)
    private double balance;
    @Column(name = "create_date")
    @CreationTimestamp
    private LocalDate createDate;
    @Column(name = "current_capacity", nullable = false)
    private double currentCapacity;
    @Column(name = "full_name")
    private String fullName;
    @Column(name = "image_url")
    private String imageUrl;
    @Column(name = "max_capacity", nullable = false)
    private double maxCapacity;
    @Column(name = "mobi_phone")
    private String mobiPhone;
    @Column(name = "payment_email")
    private String paymentEmail;
    @Column(name = "title_self")
    private String titleSelf;
    @Column(nullable = false)
    private String certificate;
    @Column(name = "is_accept")
    private boolean isAccept;

    //link to Customer
    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    //link to Course
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference
    private List<Course> courses;

    //link to Withdraw
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference
    private List<Withdraw> withdrawList;


}
