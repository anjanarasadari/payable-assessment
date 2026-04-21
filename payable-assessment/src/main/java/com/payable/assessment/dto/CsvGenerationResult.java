package com.payable.assessment.dto;

import com.payable.assessment.model.OutputRow;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CsvGenerationResult {
    private List<OutputRow> rows;
    private List<String> skippedReasons;

    public int getProcessedCount() {
        return rows.size();
    }

    public int getSkippedCount() {
        return skippedReasons.size();
    }
}
