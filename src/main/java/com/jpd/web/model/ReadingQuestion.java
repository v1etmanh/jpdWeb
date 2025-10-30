package com.jpd.web.model;

import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Collate;
import org.springframework.boot.context.properties.bind.Name;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data

public class ReadingQuestion  {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="rq_id")
	private long rqId;
    private String question;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "reading_question_options",
        joinColumns = @JoinColumn(name = "rq_id", nullable = false)
    )
    @ToString.Exclude
    private List<ReadingQuestionOptions> readingQuestionOptions;
    
    @ManyToOne
    @JoinColumn(name = "mc_id")
    @JsonBackReference
    @ToString.Exclude
    private Passage passage;
    private String feedback;

}
