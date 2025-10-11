package com.jpd.web.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Check(constraints = "rate BETWEEN 1 AND 5")
@Table(name = "feed_back")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private long feedbackId;
    @Lob
    private String content;
    @Min(1)
    @Max(5)
    private int rate;

    //link to Enrollment
    @OneToOne
    @JoinColumn(name = "enrollment_id")
    @JsonBackReference("enrollment-feedback")
    private Enrollment enrollment;


}
