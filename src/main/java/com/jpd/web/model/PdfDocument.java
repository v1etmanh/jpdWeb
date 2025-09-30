package com.jpd.web.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@DiscriminatorValue("PDF")
public class PdfDocument extends ModuleContent {
    @Column(name = "doc_url")
    private String docUrl;

}
