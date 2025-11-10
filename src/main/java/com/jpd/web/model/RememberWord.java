package com.jpd.web.model;


import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "remember_word")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RememberWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   
    private long id;
    private String word;
    private String meaning;
    @Column(columnDefinition = "LONGTEXT", name = "description")
    @Lob
    private String description;
    @ElementCollection
    @CollectionTable(
        name = "remember_word_synonym",
        joinColumns = @JoinColumn(name = "id")
    )
    
    private List<String> synonyms = new ArrayList<>();
    @ElementCollection
    @CollectionTable(
        name = "remember_word_example",
        joinColumns = @JoinColumn(name = "id")
    )
    @Column(columnDefinition = "LONGTEXT", name = "example")
    @Lob
    private List<String> example = new ArrayList<>();
    

    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference
    private Customer customer;
    @ElementCollection
    @CollectionTable(
        name = "remember_word_vote",
        joinColumns = @JoinColumn(name = "id")
    )
   
    private List<Long> vote = new ArrayList<>();
    private Language language;

}
