package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@RequiredArgsConstructor
@DiscriminatorValue("MULTIPLE_CHOICE")
@AllArgsConstructor
@Data
@Builder

public class MultipleChoiceQuestion extends ModuleContent {
    @Lob
    private String questionText;
    @Lob
    private String feedback;
    //link to MultipleChoiceOption
    @OneToMany(mappedBy = "multiple_choice_question",fetch = FetchType.EAGER, cascade = CascadeType.ALL,orphanRemoval=true)

    @JsonManagedReference
    @ToString.Exclude  // ← THÊM annotation này
    private List<MultipleChoiceOption> options;
     

}
