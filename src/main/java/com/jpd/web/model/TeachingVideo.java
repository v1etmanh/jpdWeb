package com.jpd.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

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
@DiscriminatorValue("VIDEO")
public class TeachingVideo extends ModuleContent {
	
    @Column(name = "title_video")
    private String titleVideo;
    private String videoUrl;
    
    private double durationMinutes;
}
