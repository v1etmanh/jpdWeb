package com.jpd.web.model;


import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@DiscriminatorValue("SPEAKING_PASSAGE")
@AllArgsConstructor
@NoArgsConstructor
public class SpeakingPassageQuestion extends ModuleContent {

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String passage;
    private String title;


}
