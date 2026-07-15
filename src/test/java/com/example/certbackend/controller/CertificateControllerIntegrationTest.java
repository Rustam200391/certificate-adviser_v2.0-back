package com.example.certbackend.controller;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.entity.Certificate;
import com.example.certbackend.repository.CertificateRepository;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CertificateControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CertificateRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void create_shouldPersistCertificateToDatabase() throws Exception {
        CertificateCreateDto dto = new CertificateCreateDto();
        dto.setPatientFirstName("Иван");
        dto.setPatientLastName("Иванов");
        dto.setDoctorFirstName("Пётр");
        dto.setDoctorLastName("Петров");
        dto.setDoctorSpecialization("Терапевт");

        MockMultipartFile file = new MockMultipartFile(
                "file", "cert.pdf", "application/pdf", "pdf-content".getBytes()
        );
        MockMultipartFile dtopart = new MockMultipartFile(
                "dto", "", "application/json", objectMapper.writeValueAsBytes(dto)
        );

        mockMvc.perform(multipart(POST, "/certificates")
                        .file(file)
                        .file(dtopart))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.patientFirstName").value("Иван"))
                .andExpect(jsonPath("$.patientLastName").value("Иванов"));

        List<Certificate> all = repository.findAll();
        assertThat(all).hasSize(1);
        Certificate saved = all.get(0);
        assertThat(saved.getPatientFirstName()).isEqualTo("Иван");
        assertThat(saved.getPatientLastName()).isEqualTo("Иванов");
        assertThat(saved.getDoctorFirstName()).isEqualTo("Пётр");
        assertThat(saved.getDoctorLastName()).isEqualTo("Петров");
        assertThat(saved.getDoctorSpecialization()).isEqualTo("Терапевт");
    }
}
