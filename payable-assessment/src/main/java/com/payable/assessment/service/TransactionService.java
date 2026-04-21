package com.payable.assessment.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.payable.assessment.dto.CsvGenerationResult;
import com.payable.assessment.exception.BinMasterNotLoadedException;
import com.payable.assessment.exception.InvalidFileException;
import com.payable.assessment.model.OutputRow;
import com.payable.assessment.model.TransactionRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final BigDecimal LOCAL_MDR   = new BigDecimal("2.2");
    private static final BigDecimal FOREIGN_MDR = new BigDecimal("3.5");
    private static final BigDecimal HUNDRED     = new BigDecimal("100");
    private static final String     CURRENCY    = "LKR";
    private static final String     OUTPUT_FILE_NAME = "generated_data_set.csv";

    // Supported statuses (uppercase for comparison)
    private static final Set<String> SUPPORTED_STATUSES = Set.of(
            "CAPTURED", "PARTIALLY CAPTURED", "REFUNDED", "PARTIALLY REFUNDED"
    );

    private final BinMasterService binMasterService;

    // ── File validation ───────────────────────────────────────────────────────

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Transaction CSV file is missing or empty.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileException(
                    "Invalid file type. Only .csv files are accepted for transactions.");
        }
    }

    // ── Main processing ───────────────────────────────────────────────────────

    /**
     * Parses the transaction CSV, applies business rules, and returns
     * transformed output rows plus a list of skipped row reasons.
     */
    public CsvGenerationResult processTransactions(MultipartFile file) {
        validateFile(file);

        if (!binMasterService.isBinMasterLoaded()) {
            throw new BinMasterNotLoadedException(
                    "BIN master has not been loaded. Please upload the BIN master Excel file first " +
                    "via POST /api/v1/bin-master/upload.");
        }

        List<OutputRow> outputRows    = new ArrayList<>();
        List<String>    skippedReasons = new ArrayList<>();

        try {
            // Read input stream and handle potential BOM
            byte[] bytes = file.getBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            java.io.StringReader reader = new java.io.StringReader(content);

            HeaderColumnNameMappingStrategy<TransactionRow> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(TransactionRow.class);
            // In OpenCSV 5.x, the strategy doesn't have a simple case-insensitive flag
            // BUT we can wrap it or ensure the mapping is handled.
            // Actually, the issue might be that it's NOT parsing because of the BOM or some invisible chars.
            
            CsvToBean<TransactionRow> csvToBean = new CsvToBeanBuilder<TransactionRow>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .withThrowExceptions(false) 
                    .build();

            List<TransactionRow> inputRows = csvToBean.parse();
            
            if (inputRows.isEmpty()) {
                // If it's still empty, let's try a simpler approach if the headers were slightly off
                // Or maybe they used different separators? But CSV usually means comma.
                throw new InvalidFileException("Transaction CSV file contains no data rows or headers could not be matched. Please check column names.");
            }

            for (int i = 0; i < inputRows.size(); i++) {
                TransactionRow row = inputRows.get(i);
                int csvLine = i + 2; // 1-based, +1 for header

                // ── Validate required fields ──────────────────────────────────

                if (isBlank(row.getOrderStatus())) {
                    skippedReasons.add("Row " + csvLine + ": missing Order Status");
                    continue;
                }

                String statusUpper = row.getOrderStatus().trim().toUpperCase();

                // ── Skip unsupported statuses ─────────────────────────────────
                if (!SUPPORTED_STATUSES.contains(statusUpper)) {
                    skippedReasons.add("Row " + csvLine + " (OrderID=" + row.getOrderId()
                            + "): unsupported Order Status '" + row.getOrderStatus() + "'");
                    continue;
                }

                // ── Validate account identifier ───────────────────────────────
                if (isBlank(row.getAccountIdentifier()) || row.getAccountIdentifier().trim().length() < 6) {
                    skippedReasons.add("Row " + csvLine + " (OrderID=" + row.getOrderId()
                            + "): invalid or missing Account Identifier");
                    continue;
                }

                // ── Validate amounts ──────────────────────────────────────────
                BigDecimal gross;
                BigDecimal requestAmt;
                try {
                    gross      = parseAmount(row.getCapturedAmount());
                    requestAmt = parseAmount(row.getOrderAmount());
                } catch (NumberFormatException e) {
                    skippedReasons.add("Row " + csvLine + " (OrderID=" + row.getOrderId()
                            + "): invalid amount value — " + e.getMessage());
                    continue;
                }

                // ── Build output row ──────────────────────────────────────────
                String rowType = resolveRowType(statusUpper);
                boolean isLocal = binMasterService.isLocalBin(row.getAccountIdentifier());
                BigDecimal mdr = isLocal ? LOCAL_MDR : FOREIGN_MDR;

                // Round fee first, THEN subtract to get net (as per spec)
                BigDecimal fee = gross.multiply(mdr)
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                BigDecimal net = gross.subtract(fee);

                outputRows.add(new OutputRow(
                        rowType,
                        row.getOrderDate(),
                        row.getLastUpdatedDate(),
                        row.getOrderId(),
                        formatAmount(requestAmt),
                        formatAmount(gross),
                        CURRENCY,
                        formatAmount(fee),
                        formatAmount(net)
                ));
            }

            // Save the generated CSV to the project root
            byte[] csvBytes = writeCsv(outputRows);
            try (FileOutputStream fos = new FileOutputStream(new File(OUTPUT_FILE_NAME))) {
                fos.write(csvBytes);
            }

        } catch (InvalidFileException | BinMasterNotLoadedException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidFileException("Failed to save generated CSV: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidFileException("Failed to parse transaction CSV: " + e.getMessage());
        }

        return new CsvGenerationResult(outputRows, skippedReasons);
    }

    // ── CSV writer ────────────────────────────────────────────────────────────

    /**
     * Serialises the output rows into a UTF-8 CSV byte array with
     * the exact column order required by the specification.
     */
    public byte[] writeCsv(List<OutputRow> rows) {
        if (rows == null) rows = new ArrayList<>();
        
        String[] header = {
                "ROW_TYPE", "CREATION_DATE", "APPROVED_DATE", "TRANSACTION_ID",
                "REQUEST_AMOUNT", "GROSS_LOCAL_AMOUNT", "LOCAL_CURRENCY",
                "FEE_LOCAL_AMOUNT", "NET_LOCAL_AMOUNT"
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter csv  = new CSVWriter(writer)) {

            csv.writeNext(header);

            for (OutputRow row : rows) {
                csv.writeNext(new String[]{
                        row.getRowType(),
                        row.getCreationDate(),
                        row.getApprovedDate(),
                        row.getTransactionId(),
                        row.getRequestAmount(),
                        row.getGrossLocalAmount(),
                        row.getLocalCurrency(),
                        row.getFeeLocalAmount(),
                        row.getNetLocalAmount()
                });
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to write output CSV: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps Order Status (already uppercased) to ROW_TYPE.
     * Returns null only if status is unsupported — callers skip those rows first.
     */
    String resolveRowType(String statusUpper) {
        return switch (statusUpper) {
            case "CAPTURED", "PARTIALLY CAPTURED"   -> "PAYMENT";
            case "REFUNDED", "PARTIALLY REFUNDED"   -> "REFUND";
            default -> null;
        };
    }

    /**
     * Strips commas (e.g. "21,160.00" → "21160.00") then parses as BigDecimal.
     */
    BigDecimal parseAmount(String raw) {
        if (isBlank(raw)) {
            throw new NumberFormatException("Amount value is blank");
        }
        return new BigDecimal(raw.replaceAll(",", "").trim());
    }

    /** Formats a BigDecimal to exactly 2 decimal places, no trailing zeros. */
    String formatAmount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
