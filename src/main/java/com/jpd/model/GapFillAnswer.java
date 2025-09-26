package com.jpd.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.patterns.TypePatternQuestions;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class GapFillAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private long answerId;

    //link to GapFillQuestion
    @ManyToOne
    @JoinColumn(name = "id")
    @JsonBackReference
    private GapFillQuestion gapfillQuestion;
    private String answer;
}
