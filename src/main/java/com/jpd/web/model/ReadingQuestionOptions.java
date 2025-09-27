package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Column(nullable = false)
    private boolean isCorrect;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_question_id", nullable = false)
    private ReadingQuestion readingQuestion;


}
