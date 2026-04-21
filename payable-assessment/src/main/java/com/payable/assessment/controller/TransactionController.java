package com.payable.assessment.controller;

import com.payable.assessment.dto.CsvGenerationResult;
import com.payable.assessment.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * API 2 — Generate CSV
     * POST /api/v1/transactions/generate-csv
     * Accepts: multipart/form-data, field name: file (.csv)
     * Returns: downloadable generated_data_set.csv
     */
    @PostMapping("/generate-csv")
    public ResponseEntity<byte[]> generateCsv(
            @RequestParam("file") MultipartFile file) {

        CsvGenerationResult result = transactionService.processTransactions(file);
        byte[] csvBytes = transactionService.writeCsv(result.getRows());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"generated_data_set.csv\"")
                .header("X-Rows-Processed", String.valueOf(result.getProcessedCount()))
                .header("X-Rows-Skipped",   String.valueOf(result.getSkippedCount()))
                .header("X-Skipped-Details", String.join(" | ", result.getSkippedReasons()))
                .body(csvBytes);
    }
}
