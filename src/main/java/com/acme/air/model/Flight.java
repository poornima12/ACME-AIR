package com.acme.air.model;

import jakarta.persistence.*;

@Entity
public class Flight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String flightCode; // e.g., NZ101

    @ManyToOne
    private Airport origin;

    @ManyToOne
    private Airport destination;

    private String airline; // Optional, e.g., Air New Zealand
}
