package com.jpd.web.model;

import org.springframework.data.convert.ReadingConverter;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Report {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private long reportId;
@Enumerated(EnumType.STRING)
private ReportType type;
private String detail;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "course_id")
private Course course;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private Customer customer;
}
