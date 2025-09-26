package com.jpd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
    private String audioUrl;


    //link to ListeningChoiceOption
    @OneToMany(mappedBy="question", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<ListeningChoiceOption> options;
}
