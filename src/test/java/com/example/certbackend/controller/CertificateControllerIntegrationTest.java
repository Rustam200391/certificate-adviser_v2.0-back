package com.example.certbackend.controller;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.dto.QrPlacementDto;
import com.example.certbackend.entity.Certificate;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
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
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:certificates-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
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
        mockMvc.perform(multipart(POST, "/api/certificates")
                        .file(file)
                        .param("dto", objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.patientFirstName").value("Иван"))
                .andExpect(jsonPath("$.patientLastName").value("Иванов"));

        List<Certificate> all = repository.findAll();
        assertThat(all).hasSize(1);
        String createdAt = all.get(0).getCreatedAt().toString();

        mockMvc.perform(get("/api/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdAt").value(createdAt));

        mockMvc.perform(get("/api/certificates/{id}", all.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").value(createdAt));

        Certificate saved = all.get(0);
        assertThat(saved.getPatientFirstName()).isEqualTo("Иван");
        assertThat(saved.getPatientLastName()).isEqualTo("Иванов");
        assertThat(saved.getDoctorFirstName()).isEqualTo("Пётр");
        assertThat(saved.getDoctorLastName()).isEqualTo("Петров");
        assertThat(saved.getDoctorSpecialization()).isEqualTo("Терапевт");
        assertThat(saved.getCertificateData()).isEqualTo("pdf-content".getBytes());
    }

    @Test
    void create_withoutFile_shouldPersistCertificateAndReturnId() throws Exception {
        CertificateCreateDto dto = new CertificateCreateDto();
        dto.setPatientFirstName("Anton");
        dto.setPatientLastName("Golocuckov");
        dto.setDoctorFirstName("Vugar");
        dto.setDoctorLastName("Rez");
        dto.setDoctorSpecialization("Surgeon");

        String response = mockMvc.perform(multipart(POST, "/api/certificates")
                        .param("dto", objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.patientFirstName").value("Anton"))
                .andReturn().getResponse().getContentAsString();

        Certificate saved = objectMapper.readValue(response, Certificate.class);
        assertThat(saved.getId()).isNotNull();
        Certificate persisted = repository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getCertificateData()).isNull();
    }

    @Test
    void imageUpload_updatesExistingCertificateAndRejectsInvalidPng() throws Exception {
        Certificate certificate = createCertificate();
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
        MockMultipartFile image = new MockMultipartFile("file", "cert.png", "image/png", png);

        mockMvc.perform(multipart(POST, "/api/certificates/{id}/image", certificate.getId())
                        .file(image).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(certificate.getId()))
                .andExpect(jsonPath("$.patientFirstName").value("Test"));

        Certificate updated = repository.findById(certificate.getId()).orElseThrow();
        assertThat(updated.getCertificateData()).isEqualTo(png);
        assertThat(updated.getCreatedAt()).isEqualTo(certificate.getCreatedAt());

        mockMvc.perform(get("/api/certificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentData").doesNotExist());

        MockMultipartFile notPng = new MockMultipartFile("file", "bad.png", "image/png", "bad".getBytes());
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/image", certificate.getId())
                        .file(notPng).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());

        MockMultipartFile emptyPng = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/image", certificate.getId())
                        .file(emptyPng).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart(POST, "/api/certificates/{id}/image", 99999L)
                        .file(image).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isNotFound());
    }

    @Test
    void documentUploadIsStoredSeparatelyAndDownloadedAsPdf() throws Exception {
        Certificate certificate = createCertificate();
        byte[] pdf = createPdf(2, 90);
        MockMultipartFile document = new MockMultipartFile("file", "medical.pdf", "application/pdf", pdf);
        MockMultipartFile placement = placementPart(0.68, 0.80, 0.12, "display-cropbox-top-left-normalized-v1");

        mockMvc.perform(multipart(POST, "/api/certificates/{id}/document", certificate.getId())
                        .file(document).file(placement).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentData").doesNotExist())
                .andExpect(jsonPath("$.documentName").value("medical.pdf"))
                .andExpect(jsonPath("$.documentContentType").value("application/pdf"));

        Certificate updated = repository.findById(certificate.getId()).orElseThrow();
        assertThat(updated.getDocumentData()).isNotEqualTo(pdf);
        assertThat(updated.getCertificateData()).isEqualTo("initial-image".getBytes());
        assertThat(updated.getId()).isEqualTo(certificate.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(certificate.getCreatedAt());
        assertThat(updated.getPatientFirstName()).isEqualTo(certificate.getPatientFirstName());
        assertThat(updated.getDoctorLastName()).isEqualTo(certificate.getDoctorLastName());
        mockMvc.perform(get("/api/certificates/{id}/document", certificate.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(updated.getDocumentData()));

        try (PDDocument resultPdf = Loader.loadPDF(updated.getDocumentData())) {
            assertThat(resultPdf.getNumberOfPages()).isEqualTo(2);
            assertThat(imageCount(resultPdf.getPage(0))).isEqualTo(1);
            assertThat(imageCount(resultPdf.getPage(1))).isZero();
        }
        QrScan firstPlacement = scanQr(updated.getDocumentData());
        assertThat(firstPlacement.text()).isEqualTo("http://localhost:5173/certificate/" + certificate.getId());

        MockMultipartFile movedPlacement = placementPart(0.10, 0.10, 0.12, "display-cropbox-top-left-normalized-v1");
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/document", certificate.getId())
                        .file(document).file(movedPlacement).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());
        QrScan moved = scanQr(repository.findById(certificate.getId()).orElseThrow().getDocumentData());
        assertThat(moved.text()).isEqualTo(firstPlacement.text());
        assertThat(moved.centerX()).isLessThan(firstPlacement.centerX());
        assertThat(moved.centerY()).isLessThan(firstPlacement.centerY());

        MockMultipartFile outOfBounds = placementPart(0.95, 0.95, 0.12, "display-cropbox-top-left-normalized-v1");
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/document", certificate.getId())
                        .file(document).file(outOfBounds).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());
        MockMultipartFile wrongSpace = placementPart(0.2, 0.2, 0.12, "top-left-normalized-v1");
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/document", certificate.getId())
                        .file(document).file(wrongSpace).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());

        MockMultipartFile invalidPdf = new MockMultipartFile("file", "bad.pdf", "application/pdf", "not pdf".getBytes());
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/document", certificate.getId())
                        .file(invalidPdf).file(placement).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());

        MockMultipartFile emptyPdf = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        mockMvc.perform(multipart(POST, "/api/certificates/{id}/document", certificate.getId())
                        .file(emptyPdf).file(placement).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/certificates/{id}/document", 99999L))
                .andExpect(status().isNotFound());
        Certificate withoutDocument = createCertificate();
        mockMvc.perform(get("/api/certificates/{id}/document", withoutDocument.getId()))
                .andExpect(status().isNotFound());
    }

    private Certificate createCertificate() throws Exception {
        CertificateCreateDto dto = new CertificateCreateDto();
        dto.setPatientFirstName("Test");
        dto.setPatientLastName("Patient");
        dto.setDoctorFirstName("Test");
        dto.setDoctorLastName("Doctor");
        MockMultipartFile initialImage = new MockMultipartFile("file", "original.png", "image/png", "initial-image".getBytes());
        mockMvc.perform(multipart(POST, "/api/certificates")
                        .file(initialImage).param("dto", objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
        return repository.findAll().stream().max(java.util.Comparator.comparing(Certificate::getId)).orElseThrow();
    }

    private MockMultipartFile placementPart(double x, double y, double size, String coordinateSpace) throws Exception {
        String json = "{\"pageIndex\":0,\"coordinateSpace\":\"" + coordinateSpace
                + "\",\"x\":" + x + ",\"y\":" + y + ",\"size\":" + size + "}";
        return new MockMultipartFile("placement", "", "application/json", json.getBytes());
    }

    private byte[] createPdf(int pageCount, int rotation) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage(new PDRectangle(600, 800));
                page.setCropBox(new PDRectangle(20, 30, 560, 740));
                page.setRotation(rotation);
                document.addPage(page);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private int imageCount(PDPage page) throws Exception {
        if (page.getResources() == null) return 0;
        int count = 0;
        for (var name : page.getResources().getXObjectNames()) {
            PDXObject object = page.getResources().getXObject(name);
            if (object instanceof PDImageXObject) count++;
        }
        return count;
    }

    private QrScan scanQr(byte[] pdfBytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            BufferedImage page = new PDFRenderer(document).renderImageWithDPI(0, 144);
            var result = new MultiFormatReader().decode(new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(page))));
            double centerX = 0;
            double centerY = 0;
            for (var point : result.getResultPoints()) {
                centerX += point.getX();
                centerY += point.getY();
            }
            return new QrScan(result.getText(), centerX / result.getResultPoints().length,
                    centerY / result.getResultPoints().length);
        }
    }

    private record QrScan(String text, double centerX, double centerY) { }
}
