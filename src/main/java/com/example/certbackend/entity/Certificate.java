package com.example.certbackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Patient first name is required")
    @Column(name = "patient_first_name", nullable = false, length = 100)
    private String patientFirstName;

    @NotBlank(message = "Patient last name is required")
    @Column(name = "patient_last_name", nullable = false, length = 100)
    private String patientLastName;

    @NotBlank(message = "Doctor first name is required")
    @Column(name = "doctor_first_name", nullable = false, length = 100)
    private String doctorFirstName;

    @NotBlank(message = "Doctor last name is required")
    @Column(name = "doctor_last_name", nullable = false, length = 100)
    private String doctorLastName;

    @Column(name = "doctor_specialization", length = 150)
    private String doctorSpecialization;

    @Column(name = "certificate_data", columnDefinition = "TEXT")
    private String certificateData; // base64-encoded image
}
