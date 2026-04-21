package com.payable.assessment.controller;

import com.payable.assessment.dto.BinUploadResponse;
import com.payable.assessment.service.BinMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/bin-master")
@RequiredArgsConstructor
public class BinMasterController {

    private final BinMasterService binMasterService;

    /**
     * API 1 — Upload BIN Master
     * POST /api/v1/bin-master/upload
     * Accepts: multipart/form-data, field name: file (.xlsx)
     */
    @PostMapping("/upload")
    public ResponseEntity<BinUploadResponse> uploadBinMaster(
            @RequestParam("file") MultipartFile file) {

        int totalLoaded = binMasterService.loadBins(file);

        return ResponseEntity.ok(new BinUploadResponse(
                "BIN master uploaded successfully",
                totalLoaded
        ));
    }
}
