package com.jpd.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@Data
@RequiredArgsConstructor
@DiscriminatorValue("WRITING")
@AllArgsConstructor
public class WritingQuestion extends ModuleContent {
    private String question;
    private List<String> requirements;
    @Column(table = "url_image")
    private String imageUrl;

}
