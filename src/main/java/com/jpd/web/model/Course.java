package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "course",
        indexes = @Index(columnList = "creator_id"))
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private long courseId;
    private String language;

    
    private String description;

    @Column(name = "isban", nullable = false)
    private boolean isBan;
    @Column(name = "ispublic", nullable = false)
    private boolean isPublic;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;
    @UpdateTimestamp
    @Column(name = "last_update")
    private Date lastUpdate;
    @Column(name = "learning_object")
    private String learningObject;
   
    private String name;
    private double price;
    private String requirements;
   

    @Column(name = "target_audience")
    private String targetAudience;
   
   
    @Column(name = "url_img")
    private String urlImg;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false)
    private AccessMode accessMode;
    @Column(name = "join_key")
    private String joinKey;

    //link to Creator
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonBackReference
    private Creator creator;

    //Link to chapter
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    @JsonManagedReference
    private List<Chapter> chapters;

    //Link to Enrollment
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    private List<Enrollment> enrollments;

 

    //link to Wishlist
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    private List<Wishlist> wishlists;

}
