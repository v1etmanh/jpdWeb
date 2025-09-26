package com.jpd.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name="course",
        indexes = @Index(columnList="creator_id"))
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="course_id")
    private Long id;
    private Byte language;

    private Double capacity;
    private String description;

    @Column(name="isban", nullable=false) private Boolean isBan;
    @Column(name="ispublic", nullable=false) private Boolean isPublic;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDate createdAt;
    @UpdateTimestamp
    @Column(name="last_update") private LocalDate lastUpdate;
    @Column(name="learning_object") private String learningObject;
    @Column(name="learning_outcomes") private byte[] learningOutcomes;
    private String name;
    private Double price;
    private byte[] requirements;
    private Double revenue;

    @Column(name="target_audience") private String targetAudience;
    @Column(name="teaching_language") private String teachingLanguage;
    @Column(name="total_rating") private Integer totalRating;
    @Column(name="url_img") private String urlImg;

    @Enumerated(EnumType.STRING)
    @Column(name="access_mode", nullable=false)
    private AccessMode accessMode;
    @Column(name="join_key") private String joinKey;

    //link to Creator
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;


    //Link to chapter
    @OneToMany(cascade = CascadeType.ALL,mappedBy = "course")
    private List<Chapter> chapters;

    //Link to Enrollment
    @OneToMany(cascade = CascadeType.ALL,mappedBy = "course")
    private List<Enrollment> enrollments;

    //Link to Progress_course
    @OneToMany(cascade = CascadeType.ALL,mappedBy = "course")
    private List<ProgressCourse> progressCourses;

}
