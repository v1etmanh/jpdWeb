package com.jpd.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity @Table(name="chapter",
        indexes = @Index( columnList="course_id"))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Chapter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="chapter_id")
    private Long id;

    @Column(name="chapter_name")
    private String ChapterName;
    //link to Course
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Module> modules;

    @Column(name="order_in_course")
    private Integer orderInCourse;
}
