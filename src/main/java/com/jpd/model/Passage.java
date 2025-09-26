package com.jpd.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@DiscriminatorValue("PASSAGE")
@Data
public class Passage extends ModuleContent {
    private String title;
    private String content;
}
