package com.jpd.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@RequiredArgsConstructor
@DiscriminatorValue("MULTIPLE_CHOICE")
@AllArgsConstructor
@Data
@Builder
public class Multiple_choice_Question extends ModuleContent
{
    @Lob
   private   String questionText;
    @Lob
    private String feedback;
    //link to Multiple_choice_option
    @OneToMany(mappedBy = "multiple_choice_question", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonBackReference
    private List<Multiple_choice_option> options;


}
