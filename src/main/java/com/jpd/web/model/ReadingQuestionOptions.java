package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "reading_question_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingQuestionOptions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "option_text")
    private String optionText;
    
    private boolean isCorrect;
    
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rq_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private ReadingQuestion readingQuestion;


}
