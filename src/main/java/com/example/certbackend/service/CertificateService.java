package com.example.certbackend.service;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.entity.Certificate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.certbackend.repository.CertificateRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository repository;

    /** Return all certificates. */
    @Transactional(readOnly = true)
    public List<Certificate> getAll() {
        return repository.findAll();
    }

    /** Return a single certificate by id. */
    @Transactional(readOnly = true)
    public Optional<Certificate> getById(Long id) {
        return repository.findById(id);
    }

    /** Persist a new certificate from DTO. */
    public Certificate save(CertificateCreateDto dto) {
        Certificate certificate = new Certificate();
        certificate.setPatientFirstName(dto.getPatientFirstName());
        certificate.setPatientLastName(dto.getPatientLastName());
        certificate.setDoctorFirstName(dto.getDoctorFirstName());
        certificate.setDoctorLastName(dto.getDoctorLastName());
        certificate.setDoctorSpecialization(dto.getDoctorSpecialization());
        certificate.setCertificateData(dto.getCertificateData());
        return repository.save(certificate);
    }

    /** Delete a certificate by id. */
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
