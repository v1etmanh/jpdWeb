package com.jpd.web.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@CreationTimestamp
  private LocalDateTime createDate;
    //link to ModuleContent
    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
   //@JsonManagedReference("module_modulecontent")
    @JsonIgnore
    private List<ModuleContent> moduleContent;
    @Column(name = "order_in_chapter")
    private int orderInChapter;
    @Transient
    @JsonProperty("contentTypes")
    public Set<TypeOfContent> getContentTypes() {
        if (moduleContent == null || moduleContent.isEmpty()) {
            return Set.of();
        }
        return moduleContent.stream()
                .map(ModuleContent::getTypeOfContent)
                .filter(type -> type != null)
                .collect(Collectors.toSet());
    }
    @OneToMany(mappedBy = "module",cascade = CascadeType.ALL)
    @JsonManagedReference("module-cm")
     
     private List<CustomerModuleContent>customerModuleContents;
     
    
}
