package com.payable.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BinUploadResponse {
    private String message;
    private int totalBinsLoaded;
}
