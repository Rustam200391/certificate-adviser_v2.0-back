package com.example.certbackend.controller;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.entity.Certificate;
import com.example.certbackend.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "API для управления сертификатами")
public class CertificateController {

    private final CertificateService service;

    @Operation(summary = "Получить все сертификаты")
    @ApiResponse(responseCode = "200", description = "Список сертификатов",
            content = @Content(schema = @Schema(implementation = Certificate.class)))
    @GetMapping
    public List<Certificate> getAll() {
        return service.getAll();
    }

    @Operation(summary = "Получить сертификат по ID")
    @ApiResponse(responseCode = "200", description = "Сертификат найден",
            content = @Content(schema = @Schema(implementation = Certificate.class)))
    @ApiResponse(responseCode = "404", description = "Сертификат не найден", content = @Content)
    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getById(
            @Parameter(description = "ID сертификата") @PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Создать новый сертификат")
    @ApiResponse(responseCode = "200", description = "Сертификат создан",
            content = @Content(schema = @Schema(implementation = Certificate.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Certificate> create(
            @Parameter(description = "Данные сертификата") @Valid @RequestPart CertificateCreateDto dto,
            @Parameter(description = "Файл сертификата", schema = @Schema(type = "string", format = "binary")) @RequestParam MultipartFile file) {
        Certificate saved = service.save(dto, file);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Удалить сертификат по ID")
    @ApiResponse(responseCode = "204", description = "Сертификат удалён", content = @Content)
    @ApiResponse(responseCode = "404", description = "Сертификат не найден", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID сертификата") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
