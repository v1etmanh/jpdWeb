package com.jpd.web.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KahootListFunction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="kahoot_id")
    private long  kahootId;

    
    private String title;

    //link to Chapter
   
@CreationTimestamp
  private LocalDateTime createDate;
    //link to ModuleContent
    @OneToMany(mappedBy = "kahootListFunction", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
   //@JsonManagedReference("module_modulecontent")
    @JsonManagedReference
    private List<ModuleContent> moduleContent;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    @JsonBackReference("creator-kahoot")
    private Creator creator;
    
}
