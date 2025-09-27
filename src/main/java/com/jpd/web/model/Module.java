package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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
    private long id;

    @Column(name = "title_of_module")
    private String titleOfModule;

    //link to Chapter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;


    //link to Customer_module
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerModule> customerModules;

    //link to ModuleContent
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModuleContent> moduleContent;
}
