package com.jpd.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Entity
@DiscriminatorValue("GAPFILL")
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class GapFillQuestion extends ModuleContent {
    private String questionText;
    private String feedback;
    @OneToMany(mappedBy = "gapfillQuestion",cascade = CascadeType.ALL)
    @JsonBackReference
    private List<GapFillAnswer> answers;

}
