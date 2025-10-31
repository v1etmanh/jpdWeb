package com.jpd.web.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder

public class Creator_code_changeP {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name="ccc_id")
private long cccId;
private long creatorId;
@CreationTimestamp
private LocalDateTime creaTime;
private String code;
}
