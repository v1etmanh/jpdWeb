package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Entity
@DiscriminatorValue("GAPFILL")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class GapFillQuestion extends ModuleContent {
    private String questionText;
    private String feedback;

    //link to GapFillAnswer
    @OneToMany(fetch = FetchType.EAGER,mappedBy = "gapfillQuestion",orphanRemoval = true, cascade = CascadeType.ALL)

    @JsonManagedReference
    @ToString.Exclude
    private List<GapFillAnswer> answers;

}
