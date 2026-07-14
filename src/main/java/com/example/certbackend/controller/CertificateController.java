package com.example.certbackend.controller;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.entity.Certificate;
import com.example.certbackend.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/certificates")
@CrossOrigin(origins = "http://localhost:3000") // Allow React dev server
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService service;


    /**
     * GET /certificates
     * Returns all saved certificates.
     */
    @GetMapping
    public List<Certificate> getAll() {
        return service.getAll();
    }

    /**
     * GET /certificates/{id}
     * Returns a single certificate or 404.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /certificates
     * Saves a new certificate and returns it with its generated id.
     */
    @PostMapping
    public ResponseEntity<Certificate> create(@Valid @RequestPart CertificateCreateDto dto,
                                              @RequestParam MultipartFile file) {
        Certificate saved = service.save(dto, file);
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /certificates/{id}
     * Deletes a certificate by id.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
