package com.example.certbackend.controller;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.entity.Certificate;
import com.example.certbackend.service.CertificateService;
import com.example.certbackend.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CertificateController.class)
@Import(AppConfig.class)
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CertificateService service;

    @Test
    void create_shouldReturnSavedCertificate() throws Exception {
        CertificateCreateDto dto = new CertificateCreateDto();
        dto.setPatientFirstName("Иван");
        dto.setPatientLastName("Иванов");
        dto.setDoctorFirstName("Пётр");
        dto.setDoctorLastName("Петров");
        dto.setDoctorSpecialization("Терапевт");

        Certificate saved = new Certificate();
        saved.setId(1L);
        saved.setPatientFirstName(dto.getPatientFirstName());
        saved.setPatientLastName(dto.getPatientLastName());
        saved.setDoctorFirstName(dto.getDoctorFirstName());
        saved.setDoctorLastName(dto.getDoctorLastName());
        saved.setDoctorSpecialization(dto.getDoctorSpecialization());
        saved.setCreatedAt(Instant.parse("2025-01-02T03:04:05Z"));

        when(service.save(any(CertificateCreateDto.class), any())).thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "pdf-content".getBytes()
        );
        mockMvc.perform(multipart(HttpMethod.POST, "/api/certificates")
                        .file(file)
                        .param("dto", objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.createdAt").value("2025-01-02T03:04:05Z"))
                .andExpect(jsonPath("$.patientFirstName").value("Иван"))
                .andExpect(jsonPath("$.patientLastName").value("Иванов"))
                .andExpect(jsonPath("$.doctorFirstName").value("Пётр"))
                .andExpect(jsonPath("$.doctorLastName").value("Петров"))
                .andExpect(jsonPath("$.doctorSpecialization").value("Терапевт"));

        ArgumentCaptor<CertificateCreateDto> dtoCaptor = ArgumentCaptor.forClass(CertificateCreateDto.class);
        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(service).save(dtoCaptor.capture(), fileCaptor.capture());

        CertificateCreateDto capturedDto = dtoCaptor.getValue();
        assertThat(capturedDto.getPatientFirstName()).isEqualTo("Иван");
        assertThat(capturedDto.getPatientLastName()).isEqualTo("Иванов");
        assertThat(capturedDto.getDoctorFirstName()).isEqualTo("Пётр");
        assertThat(capturedDto.getDoctorLastName()).isEqualTo("Петров");
        assertThat(capturedDto.getDoctorSpecialization()).isEqualTo("Терапевт");
        assertThat(fileCaptor.getValue().getOriginalFilename()).isEqualTo("cert.pdf");
    }
}
