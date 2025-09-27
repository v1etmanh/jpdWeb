package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    private long id;
    private String language;

    private double capacity;
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
    private LocalDate lastUpdate;
    @Column(name = "learning_object")
    private String learningObject;
    @Column(name = "learning_outcomes")
    private String learningOutcomes;
    private String name;
    private double price;
    private String requirements;
    private double revenue;

    @Column(name = "target_audience")
    private String targetAudience;
    @Column(name = "teaching_language")
    private String teachingLanguage;
    @Column(name = "total_rating")
    private int totalRating;
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
    private List<Chapter> chapters;

    //Link to Enrollment
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    private List<Enrollment> enrollments;

    //Link to Progress_course
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    private List<ProgressCourse> progressCourses;

    //link to Wishlist
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course")
    private List<Wishlist> wishlists;

}
