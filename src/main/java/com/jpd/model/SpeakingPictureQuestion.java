package com.jpd.model;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@DiscriminatorValue("SPEAKING_PICTURE")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SpeakingPictureQuestion extends ModuleContent {

    private String pictureUrl;
    @OneToMany(mappedBy = "speakingPictureQuestion",cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<SpeakingPictureListQuestions>speakingPictureListQuestions;

}
