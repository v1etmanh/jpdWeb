package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Column(name = "creator_id")
    private long creatorId;
    @Column(nullable = false)
    private double balance;
    @Column(name = "create_date")
    @CreationTimestamp
    private Date createDate;

    @Column(name = "full_name")
    private String fullName;
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "mobi_phone")
    private String mobiPhone;
    @Column(name = "payment_email")
    private String paymentEmail;
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
    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    @ToString.Exclude 
    private Customer customer;

    //link to Course
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference("creator-course")
    private List<Course> courses;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference("creator-kahoot")
    private List<KahootListFunction> kahootListFunctions;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference("creator-withdraw")
    private List<Withdraw> withdrawList;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator")
    @JsonManagedReference("creator-payout")
    private List<PayoutTracking> payoutTrackings;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator",fetch = FetchType.LAZY)
    @JsonManagedReference("creator-monthlyBalance")
    private List<MonthlyCreatorBalance> monthlyBalances;
private boolean ban;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "creator",fetch = FetchType.LAZY)
    @JsonManagedReference("creator-warning")
    private List<CreatorWarning> warnings;
    @Column(name = "warning_count")
    @Builder.Default
    private Integer warningCount = 0;
    @Column(name = "banned_until")
    private Date bannedUntil;
    @Column(name = "reputation_score")
    @Builder.Default
    private Integer reputationScore = 100;
}
