package com.jpd.model;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Table(name = "customer")
@Data //Bao gồm @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;
    private LocalDate create_date = LocalDate.now();
    private String email;
    private String family_name;
    private String given_name;
    @Column(nullable = false)
    private int limit_word;
    @Column(nullable = false)
    private int number_request;
    private String role;
    private String username;
    private String password;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Creator creator;

}
