package com.jpd.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@Entity
@Table(name = "listen_choice_options")
public class ListeningChoiceOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "options_id")
    private long id;

    //link to ListeningChoiceQuestion
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id", nullable = false)
    private ListeningChoiceQuestion question;

    @Column(name = "option_text", length = 1000)
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;
}
