package com.jpd.web.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.ToString;

@Entity
@DiscriminatorValue("READING")
@Data
public class Passage extends ModuleContent {
    private String title;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    @OneToMany(mappedBy = "passage",cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonManagedReference
    @ToString.Exclude
    private List<ReadingQuestion>readingQuestion;
}
