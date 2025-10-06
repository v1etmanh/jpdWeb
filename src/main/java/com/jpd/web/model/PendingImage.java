package com.jpd.web.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.internal.build.AllowNonPortable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Data
public class PendingImage {
@Id
@Column(name = "pd_id")
@GeneratedValue(strategy = GenerationType.IDENTITY)
private long pdId;
private  long creatorId;
private String url;
private Status status;
@CreationTimestamp
private LocalDateTime createDate;
}
