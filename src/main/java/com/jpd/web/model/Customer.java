package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;


@Entity
@Table(name = "customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private int id;
    @Column(name = "create_date")
    @CreationTimestamp
    private LocalDate createDate;
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
    @JsonManagedReference
    private List<Remember_word> rememberWords;

    //link to Progress_course
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<ProgressCourse> progressCourses;

    //link to Wish_list
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Wishlist> wishlists;

    //link to Enrollment
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Enrollment> enrollments;

    //link to Customer_chapter
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<CustomerChapter> customerChapters;


    //link to Customer_module
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<CustomerModule> customerModules;


    //link to Customer_question
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<CustomerQuestion> customerQuestions;

}
