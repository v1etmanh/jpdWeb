package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.DiscriminatorOptions;

@Entity
@Table(name = "module_content",
        indexes = @Index(name = "idx_content_module_type", columnList = "module_id,type_of_content"))
@Inheritance(strategy = InheritanceType.JOINED)
// Dùng chính cột ENUM "type_of_content" làm discriminator:
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
@Data
public abstract class ModuleContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Id;
    @Enumerated(EnumType.STRING)
    private TypeOfContent typeOfContent;

    //link to Module
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ModuleContent_id", nullable = false)
    @JsonBackReference
    private Module module;


}
