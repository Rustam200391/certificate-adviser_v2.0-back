package com.example.certbackend.service;

import com.example.certbackend.dto.CertificateCreateDto;
import com.example.certbackend.dto.QrPlacementDto;
import com.example.certbackend.entity.Certificate;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.certbackend.repository.CertificateRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;

@Service
@Transactional
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository repository;

    @Value("${app.frontend-base-url:http://localhost:3000}")
    private String frontendBaseUrl;

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
    public Certificate save(CertificateCreateDto dto, MultipartFile file) {
        Certificate certificate = new Certificate();
        certificate.setPatientFirstName(dto.getPatientFirstName());
        certificate.setPatientLastName(dto.getPatientLastName());
        certificate.setDoctorFirstName(dto.getDoctorFirstName());
        certificate.setDoctorLastName(dto.getDoctorLastName());
        certificate.setDoctorSpecialization(dto.getDoctorSpecialization());
        if (file != null) {
            try {
                certificate.setCertificateData(file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return repository.save(certificate);
    }

    public Optional<Certificate> updateImage(Long id, MultipartFile file) {
        if (file == null || file.isEmpty() || !"image/png".equalsIgnoreCase(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty PNG file is required");
        }
        try {
            byte[] bytes = file.getBytes();
            byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
            if (bytes.length < signature.length || !Arrays.equals(signature, Arrays.copyOf(bytes, signature.length))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PNG signature");
            }
            return repository.findById(id).map(certificate -> {
                certificate.setCertificateData(bytes);
                return repository.save(certificate);
            });
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read PNG file", e);
        }
    }

    public Optional<Certificate> updateDocument(Long id, MultipartFile file, QrPlacementDto placement) {
        Optional<Certificate> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (file == null || file.isEmpty() || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty PDF file is required");
        }
        validatePlacement(placement);
        try {
            byte[] bytes = file.getBytes();
            byte[] signature = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            if (bytes.length < signature.length || !Arrays.equals(signature, Arrays.copyOf(bytes, signature.length))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PDF signature");
            }
            Certificate certificate = existing.get();
            byte[] processedPdf = addQrToFirstPage(bytes, certificate.getId(), placement);
            certificate.setDocumentData(processedPdf);
            certificate.setDocumentName(file.getOriginalFilename());
            certificate.setDocumentContentType("application/pdf");
            return Optional.of(repository.save(certificate));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process PDF document", e);
        } catch (Exception e) {
            if (e instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to process PDF document", e);
        }
    }

    private void validatePlacement(QrPlacementDto placement) {
        if (placement == null || placement.getPageIndex() != 0
                || !"display-cropbox-top-left-normalized-v1".equals(placement.getCoordinateSpace())
                || !Double.isFinite(placement.getX()) || !Double.isFinite(placement.getY())
                || !Double.isFinite(placement.getSize())
                || placement.getX() < 0 || placement.getX() > 1
                || placement.getY() < 0 || placement.getY() > 1
                || placement.getSize() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid QR placement");
        }
    }

    private byte[] addQrToFirstPage(byte[] sourcePdf, Long certificateId, QrPlacementDto placement) throws Exception {
        try (PDDocument document = Loader.loadPDF(sourcePdf)) {
            if (document.getNumberOfPages() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF has no pages");
            }
            PDPage page = document.getPage(0);
            PDRectangle cropBox = page.getCropBox();
            int rotation = Math.floorMod(page.getRotation(), 360);
            if (rotation % 90 != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported PDF page rotation");
            }
            double cropWidth = cropBox.getWidth();
            double cropHeight = cropBox.getHeight();
            boolean quarterTurn = rotation == 90 || rotation == 270;
            double displayWidth = quarterTurn ? cropHeight : cropWidth;
            double displayHeight = quarterTurn ? cropWidth : cropHeight;
            double side = placement.getSize() * Math.min(displayWidth, displayHeight);
            double displayX = placement.getX() * displayWidth;
            double displayY = placement.getY() * displayHeight;
            if (!Double.isFinite(side) || side <= 0 || displayX + side > displayWidth + 1e-7
                    || displayY + side > displayHeight + 1e-7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR does not fit inside the visible page");
            }

            double[][] corners = {
                    toPdfPoint(displayX, displayY, cropWidth, cropHeight, rotation),
                    toPdfPoint(displayX + side, displayY, cropWidth, cropHeight, rotation),
                    toPdfPoint(displayX, displayY + side, cropWidth, cropHeight, rotation),
                    toPdfPoint(displayX + side, displayY + side, cropWidth, cropHeight, rotation)
            };
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            for (double[] corner : corners) {
                minX = Math.min(minX, corner[0]);
                minY = Math.min(minY, corner[1]);
            }
            float pdfX = (float) (cropBox.getLowerLeftX() + minX);
            float pdfY = (float) (cropBox.getLowerLeftY() + minY);

            String qrUrl = frontendBaseUrl.replaceAll("/+$", "") + "/certificate/" + certificateId;
            com.google.zxing.common.BitMatrix qrMatrix = new QRCodeWriter()
                    .encode(qrUrl, BarcodeFormat.QR_CODE, 512, 512);
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(qrMatrix, "PNG", png);
            PDImageXObject qr = PDImageXObject.createFromByteArray(document, png.toByteArray(), "certificate-qr");
            try (PDPageContentStream content = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                content.drawImage(qr, pdfX, pdfY, (float) side, (float) side);
            }
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            document.save(result);
            return result.toByteArray();
        }
    }

    private double[] toPdfPoint(double displayX, double displayY, double cropWidth, double cropHeight, int rotation) {
        return switch (rotation) {
            case 0 -> new double[]{displayX, cropHeight - displayY};
            case 90 -> new double[]{displayY, displayX};
            case 180 -> new double[]{cropWidth - displayX, cropHeight - displayY};
            case 270 -> new double[]{cropWidth - displayY, cropHeight - displayX};
            default -> throw new IllegalArgumentException("Unsupported rotation");
        };
    }

    /** Delete a certificate by id. */
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
