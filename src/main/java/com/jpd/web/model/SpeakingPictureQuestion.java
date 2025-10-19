package com.jpd.web.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Entity
@DiscriminatorValue("SPEAKING_PICTURE")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SpeakingPictureQuestion extends ModuleContent {

    private String pictureUrl;
    @OneToMany(mappedBy = "speakingPictureQuestion", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonManagedReference
    @ToString.Exclude
    private List<SpeakingPictureListQuestions> speakingPictureListQuestions;

}
