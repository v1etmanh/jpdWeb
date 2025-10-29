package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "creator")
<<<<<<< HEAD
@Data // Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode,
      // @RequiredArgsConstructor
=======
@Data //Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
>>>>>>> jpdWeb6/master
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
<<<<<<< HEAD

=======
    @Setter(AccessLevel.NONE)
>>>>>>> jpdWeb6/master
    @Column(name = "creator_id")
    private long creatorId;
    @Column(nullable = false)
    private double balance;
    @Column(name = "create_date")
    @CreationTimestamp
    private Date createDate;
<<<<<<< HEAD

=======
    
>>>>>>> jpdWeb6/master
    @Column(name = "full_name")
    private String fullName;
    @Column(name = "image_url")
    private String imageUrl;
<<<<<<< HEAD

=======
   
>>>>>>> jpdWeb6/master
    @Column(name = "mobi_phone")
    private String mobiPhone;
    @Column(name = "payment_email")
    private String paymentEmail;
<<<<<<< HEAD
    @Column(name = "title_self")
    private String titleSelf;
    @ElementCollection
    @CollectionTable(name = "creator_certificates", joinColumns = @JoinColumn(name = "creator_id"))
    @Column(name = "certificate_url")
    private List<String> certificateUrl = new ArrayList<>();
    @Column(name = "status")
    private Status status;

    @Column(name = "reputation_score")
    @Builder.Default
    private Integer reputationScore = 100;

    @Column(name = "is_banned")
    @Builder.Default
    private Boolean isBanned = false;

    @Column(name = "banned_until")
    private Date bannedUntil;

    @Column(name = "warning_count")
    @Builder.Default
    private Integer warningCount = 0;

    // link to Customer
=======
    @Column(name = "title_self")   
    private String titleSelf;
    @ElementCollection
    @CollectionTable(
        name = "creator_certificates",
        joinColumns = @JoinColumn(name = "creator_id")
    )
    @Column(name = "certificate_url")
    private  List<String> certificateUrl= new ArrayList<>();
    @Column(name = "status")
    private Status status;

    //link to Customer
>>>>>>> jpdWeb6/master
    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "customer_id")
    private Customer customer;

<<<<<<< HEAD
    // link to Course
=======
    //link to Course
>>>>>>> jpdWeb6/master
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference
    private List<Course> courses;

<<<<<<< HEAD
    // link to Withdraw
=======
    //link to Withdraw
>>>>>>> jpdWeb6/master
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference
    private List<Withdraw> withdrawList;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
<<<<<<< HEAD
    private List<PayoutTracking> payoutTrackings;

    // link to CreatorWarning
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference
    private List<CreatorWarning> warnings;
=======
    private List<PayoutTracking>payoutTrackings ;
    
>>>>>>> jpdWeb6/master

}
