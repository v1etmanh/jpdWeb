package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@Entity
@Table(name = "listion_choice_options")
public class ListeningChoiceOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "options_id")
    private long optionId;

    //link to ListeningChoiceQuestion
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mc_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private ListeningChoiceQuestion question;

    @Column(name = "option_text", length = 1000)
    private String optionText;

    @Column( nullable = false)
    private boolean correct;
}
