package com.example.certbackend.controller;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.dto.QrPlacementDto;
import com.example.certbackend.entity.Certificate;
import com.example.certbackend.service.CertificateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/certificates", "/api/certificates"})
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:5174"
})
@Tag(name = "Certificates", description = "API для управления сертификатами")
public class CertificateController {

    private final CertificateService service;
    private final ObjectMapper objectMapper;

    public CertificateController(
            CertificateService service,
            ObjectMapper objectMapper) {

        this.service = service;
        this.objectMapper = objectMapper;

        // Support java.time.LocalDate in CertificateCreateDto
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Operation(summary = "Получить все сертификаты")
    @ApiResponse(
            responseCode = "200",
            description = "Список сертификатов",
            content = @Content(schema = @Schema(implementation = Certificate.class))
    )
    @GetMapping
    public List<Certificate> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Получить сертификат по ID")
    @ApiResponse(
            responseCode = "200",
            description = "Сертификат найден",
            content = @Content(schema = @Schema(implementation = Certificate.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Сертификат не найден",
            content = @Content
    )
    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getById(
            @Parameter(description = "ID сертификата")
            @PathVariable Long id) {

        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Создать новый сертификат")
    @ApiResponse(
            responseCode = "200",
            description = "Сертификат создан",
            content = @Content(schema = @Schema(implementation = Certificate.class))
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Certificate> create(
            @Parameter(description = "Данные сертификата в формате JSON")
            @RequestParam String dto,

            @Parameter(
                    description = "Файл сертификата",
                    schema = @Schema(type = "string", format = "binary")
            )
            @RequestParam(value = "file", required = false)
            MultipartFile file
    ) throws Exception {

        CertificateCreateDto createDto =
                objectMapper.readValue(dto, CertificateCreateDto.class);

        Certificate saved =
                service.save(createDto, file);

        return ResponseEntity.ok(saved);
    }

    @PutMapping(
            value = "/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Certificate> updateImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        return service.updateImage(id, file)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @PutMapping(
            value = "/{id}/document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Certificate> updateDocument(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestPart("placement") QrPlacementDto placement) {

        return service.updateDocument(id, file, placement)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> getDocument(
            @PathVariable Long id) {

        return service.getById(id)
                .map(certificate -> {

                    byte[] document =
                            certificate.getDocumentData();

                    if (document == null || document.length == 0) {
                        return ResponseEntity
                                .notFound()
                                .<byte[]>build();
                    }

                    String filename =
                            certificate.getDocumentName() == null
                                    ? "document.pdf"
                                    : certificate.getDocumentName();

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition
                                            .attachment()
                                            .filename(filename)
                                            .build()
                                            .toString()
                            )
                            .body(document);

                })
                .orElseGet(() ->
                        ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .build()
                );
    }

    @Operation(summary = "Удалить сертификат по ID")
    @ApiResponse(
            responseCode = "204",
            description = "Сертификат удалён",
            content = @Content
    )
    @ApiResponse(
            responseCode = "404",
            description = "Сертификат не найден",
            content = @Content
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID сертификата")
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}