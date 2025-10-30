package com.jpd.web.model;

import java.time.LocalDateTime;

import org.checkerframework.checker.units.qual.degrees;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.internal.build.AllowNonPortable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
@Entity
public class CreatorRequestNumber {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
private long numberId;
	@Column(unique = true)
private long creatorId;
private int number;

@CreationTimestamp
private LocalDateTime lastUpdate;
}
