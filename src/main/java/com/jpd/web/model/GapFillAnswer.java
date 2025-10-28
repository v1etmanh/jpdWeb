package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

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
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mc_id")
    @JsonBackReference
    @ToString.Exclude
    private GapFillQuestion gapfillQuestion;
    private String answer;
}
