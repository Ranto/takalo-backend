package ara.project.takalo.purchase.infrastructure.rest;

import ara.project.takalo.purchase.application.port.in.PurchaseImportServicePort;
import ara.project.takalo.purchase.application.port.in.PurchaseServicePort;
import ara.project.takalo.purchase.domain.exception.UnsupportedImportFormatException;
import ara.project.takalo.purchase.domain.model.ImportFormat;
import ara.project.takalo.purchase.domain.model.Purchase;
import ara.project.takalo.purchase.domain.model.PurchaseImportResult;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseImportResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseLightResponse;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseRequest;
import ara.project.takalo.purchase.infrastructure.rest.dto.PurchaseResponse;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseImportWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseLightWebMapper;
import ara.project.takalo.purchase.infrastructure.rest.mapper.PurchaseWebMapper;
import ara.project.takalo.shared.domain.utility.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseServicePort service;
    private final PurchaseImportServicePort importService;
    private final PurchaseWebMapper purchaseWebMapper;
    private final PurchaseLightWebMapper purchaseLightWebMapper;
    private final PurchaseImportWebMapper purchaseImportWebMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseResponse create(@Valid @RequestBody PurchaseRequest request) {
        Purchase purchase = purchaseWebMapper.toDomain(request);
        Purchase saved = service.create(purchase);
        return purchaseWebMapper.toResponse(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseResponse> update(@PathVariable UUID id, @Valid @RequestBody PurchaseRequest request) {
        Purchase purchase = purchaseWebMapper.toDomain(request);
        Purchase update = service.update(id, purchase);
        return ResponseEntity.ok(purchaseWebMapper.toResponse(update));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> getById(@PathVariable UUID id) {
        Purchase purchase = service.getById(id);
        return ResponseEntity.ok(purchaseWebMapper.toResponse(purchase));
    }

    @GetMapping
    public PagedResponse<PurchaseLightResponse> search(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<Purchase> result = service.search(start, end, page, size);
        return result.map(purchaseLightWebMapper::toResponse);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PurchaseImportResponse importPurchases(@RequestPart("file") MultipartFile file) {
        ImportFormat format = detectFormat(file);
        try (var stream = file.getInputStream()) {
            PurchaseImportResult result = importService.importPurchases(stream, format);
            return purchaseImportWebMapper.toResponse(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ImportFormat detectFormat(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".xlsx")) return ImportFormat.EXCEL_XLSX;
            if (lower.endsWith(".docx")) return ImportFormat.WORD;
            if (lower.endsWith(".csv")) return ImportFormat.CSV;
        }
        throw new UnsupportedImportFormatException(name);
    }

}
