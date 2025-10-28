package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class SpeakingPictureListQuestions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "speaking_picture_id")
    private long id;
    private String question;

    private String answer;
    @ManyToOne
    @JoinColumn(name = "mc_id")
    @JsonBackReference
    @ToString.Exclude
    private SpeakingPictureQuestion speakingPictureQuestion;
}