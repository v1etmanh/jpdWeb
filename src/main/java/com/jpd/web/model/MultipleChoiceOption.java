package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Entity
@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class MultipleChoiceOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mco_id")
    private long mcoId;
    private String optionText;
    @Column(nullable = false)
    private boolean isCorrect;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mc_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude  
    private MultipleChoiceQuestion multiple_choice_question;
}
