package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingQuestionOptions {
    
    @Column(name = "option_text")
    private String optionText;
    
    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;
}
