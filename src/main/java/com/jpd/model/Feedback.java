package com.jpd.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "rating BETWEEN 1 AND 5")
@Table(name="feed_back")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Lob
    private String content;
    @Min(1) @Max(5)
    private Integer rate;

    //link to Enrollment
    @OneToOne
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;


}
