package com.example.certbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateCreateDto {

    @NotBlank(message = "Patient first name is required")
    private String patientFirstName;

    @NotBlank(message = "Patient last name is required")
    private String patientLastName;

    @NotBlank(message = "Doctor first name is required")
    private String doctorFirstName;

    @NotBlank(message = "Doctor last name is required")
    private String doctorLastName;

    private String doctorSpecialization;

}
