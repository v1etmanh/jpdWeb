package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import org.hibernate.annotations.DiscriminatorOptions;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
// Dùng chính cột ENUM "type_of_content" làm discriminator:
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
@Data
public abstract class ModuleContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mc_id")
    private long mcId;
    @Enumerated(EnumType.STRING)
    private TypeOfContent typeOfContent;

    //link to Module
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonBackReference
    private Module module;
    
    @OneToMany(mappedBy = "moduleContent")
    private List<CustomerModuleContent> customerModuleContent;
    


}
