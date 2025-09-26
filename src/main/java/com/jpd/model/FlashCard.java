package com.jpd.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@DiscriminatorValue("FLASHCARD")
@Builder
@Table(name = "flash_card")
public class FlashCard extends ModuleContent {
    private String word;
    private String meaning;
    @Column(name = "img_url", length = 2048)
    private String imageUrl;
}

