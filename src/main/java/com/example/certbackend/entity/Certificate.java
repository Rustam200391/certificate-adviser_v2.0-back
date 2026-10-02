package com.example.certbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

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

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "cert_image")
    private byte[] certificateData;

    @JsonIgnore
    @Column(name = "document_data")
    private byte[] documentData;

    @Column(name = "document_name", length = 255)
    private String documentName;

    @Column(name = "document_content_type", length = 100)
    private String documentContentType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}