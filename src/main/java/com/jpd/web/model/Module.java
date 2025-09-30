package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "module",
        indexes = @Index(columnList = "chapter_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="module_id")
    private long moduleId;

    @Column(name = "title_of_module")
    private String titleOfModule;

    //link to Chapter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonBackReference
    private Chapter chapter;


    //link to ModuleContent
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
   @JsonManagedReference
    private List<ModuleContent> moduleContent;
}
