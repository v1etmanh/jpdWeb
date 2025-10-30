package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


import java.util.List;

import org.hibernate.annotations.DiscriminatorOptions;
import com.fasterxml.jackson.annotation.JsonSubTypes;
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
// Dùng chính cột ENUM "type_of_content" làm discriminator:
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)

@Data
@JsonTypeInfo(
	    use = JsonTypeInfo.Id.NAME,
	    include = JsonTypeInfo.As.EXISTING_PROPERTY,
	    property = "typeOfContent",
	    visible = true
	)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FlashCard.class, name = "FLASHCARD"),
    @JsonSubTypes.Type(value = GapFillQuestion.class, name = "GAPFILL"),
    @JsonSubTypes.Type(value = ListeningChoiceQuestion.class, name = "LISTEN_CHOICE"),
    @JsonSubTypes.Type(value = SpeakingPassageQuestion.class, name = "SPEAKING_PASSAGE"),
    @JsonSubTypes.Type(value = SpeakingPictureQuestion.class, name = "SPEAKING_PICTURE"),
    @JsonSubTypes.Type(value = PdfDocument.class, name = "PDF"),
    @JsonSubTypes.Type(value = TeachingVideo.class, name = "VIDEO"),
    @JsonSubTypes.Type(value = Passage.class, name = "READING"),
    @JsonSubTypes.Type(value = WritingQuestion.class, name = "WRITING"),
    @JsonSubTypes.Type(value = MultipleChoiceQuestion.class, name = "MULTIPLE_CHOICE")
    
    // Thêm các type khác nếu có...
})
public abstract class ModuleContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mc_id")
    protected Long mcId;
    @Enumerated(EnumType.STRING)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    protected TypeOfContent typeOfContent;

    //link to Module
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "module_id", nullable = true)
    // @JsonBackReference("module_modulecontent")
    @JsonIgnore
    protected Module module;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "kahoot_id", nullable = true)
    // @JsonBackReference("module_modulecontent")
    @JsonBackReference
    protected KahootListFunction kahootListFunction;
    

}
