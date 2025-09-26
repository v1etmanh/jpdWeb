package com.jpd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Multiple_choice_option {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int Id;
    private String optionText;
    @Column( nullable=false)
    private boolean isCorrect;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "multiple_choice_question_id", nullable = false)
    private Multiple_choice_Question multiple_choice_question;
}
