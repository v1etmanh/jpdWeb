package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name="listen_choice_question")
@DiscriminatorValue("LISTEN_CHOICE")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class ListeningChoiceQuestion extends ModuleContent {
    @Lob
    private String question;

    @Column(name="url", length=2048)
    private String imgUrl;


    //link to ListeningChoiceOption
    @OneToMany(mappedBy="question", cascade=CascadeType.ALL, orphanRemoval=true)
    @JsonManagedReference
    @ToString.Exclude
    private List<ListeningChoiceOption> options;
}
