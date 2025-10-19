package com.jpd.web.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@RequiredArgsConstructor
@DiscriminatorValue("WRITING")
@AllArgsConstructor
public class WritingQuestion extends ModuleContent {
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String question;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String requirements;
    @Column(name = "url_image")
    private String imageUrl;
    @Enumerated(EnumType.STRING)
    private TaskTypeCategory taskTypeCategory;
    @ElementCollection
    @CollectionTable(
            name = "writing_criterias",
            joinColumns = @JoinColumn(name = "mc_id")
    )
    @Column(name = "criteria")

    private List<String> criterias = new ArrayList<>();
    // table này chỉ tồn tại nếu url img !=null + this report categories
    @ElementCollection
    @CollectionTable(
            name = "writing_features",
            joinColumns = @JoinColumn(name = "mc_id")
    )
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();

}