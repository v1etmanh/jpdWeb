package com.jpd.web.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Table(
	    name = "customer_question",
	    uniqueConstraints = {
	        @UniqueConstraint(columnNames = {"enroll_id", "module_id"})
	    }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerModuleContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cq_id")
    private long cqId;

    @Column(name = "available__request", nullable = false)
    private int availableRequest;


    //link to Customer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enroll_id")
    @JsonBackReference("enrollment-customerContent")  // ← Đ
    private Enrollment enrollment;

    //link to Course
    @ElementCollection
    @CollectionTable(
        name = "finished_type_of_content", // tên bảng trung gian
        joinColumns = @JoinColumn(name = "cq_id") // khóa ngoại trỏ đến Course
    )
   private Set<TypeOfContent> typeOfContent=new HashSet<>();
   @ManyToOne()
   @JoinColumn(name = "module_id")
   @JsonBackReference("module-cm")
    private Module module;

}
