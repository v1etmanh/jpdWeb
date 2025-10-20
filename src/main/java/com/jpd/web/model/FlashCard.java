package com.jpd.web.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.*;

@Entity

@AllArgsConstructor

@DiscriminatorValue("FLASHCARD")
@RequiredArgsConstructor
@Data
public class FlashCard extends ModuleContent {
	  
	    private String word;

	   
	    private String meaning;

	    private String imgUrl;

	    
}

