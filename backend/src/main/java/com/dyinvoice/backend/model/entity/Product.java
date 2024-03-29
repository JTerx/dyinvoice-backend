package com.dyinvoice.backend.model.entity;


import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Product implements BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private double unitPrice;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;


    @Override
    public Long getId() {
        return id;
    }


}
