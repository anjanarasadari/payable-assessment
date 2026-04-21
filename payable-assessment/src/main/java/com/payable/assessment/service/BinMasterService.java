package com.payable.assessment.service;

import com.payable.assessment.exception.InvalidFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

@Service
public class BinMasterService {

    private final Set<String> localBins = new HashSet<>();
    private static final String BIN_MASTER_FILE_NAME = "Latest BIN List.xlsx";

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("BIN master file is missing or empty.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new InvalidFileException(
                    "Invalid file type. Only .xlsx files are accepted for BIN master.");
        }
    }

    /**
     * Writes the upload to a temp file first, then opens it with XSSFWorkbook.
     * This bypasses ALL content-type / MIME detection that POI does on streams,
     * which is the root cause of "Content-Type is not supported" errors.
     */
    public int loadBins(MultipartFile file) {
        validateFile(file);
        localBins.clear();

        File targetFile = new File(BIN_MASTER_FILE_NAME);
        try {
            // Save the file to the project root
            Files.copy(file.getInputStream(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            try (Workbook workbook = new XSSFWorkbook(targetFile)) {
                Sheet sheet = workbook.getSheetAt(0);

                if (sheet == null) {
                    throw new InvalidFileException("Excel file contains no sheets.");
                }

                boolean hasDataRows = false;

                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // skip header

                    Cell cell = row.getCell(0);
                    if (cell == null) continue;

                    String bin = extractBinFromCell(cell);
                    if (bin != null && !bin.isBlank()) {
                        if (!bin.matches("\\d{6}")) {
                            throw new InvalidFileException(
                                    "Invalid BIN value: '" + bin + "'. Expected 6-digit numeric BIN.");
                        }
                        localBins.add(bin);
                        hasDataRows = true;
                    }
                }

                if (!hasDataRows) {
                    throw new InvalidFileException("BIN master file contains no valid BIN data.");
                }
            }

        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidFileException("Failed to read/save BIN master file: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidFileException("Failed to parse Excel file: " + e.getMessage());
        }

        return localBins.size();
    }

    public boolean isLocalBin(String accountIdentifier) {
        if (accountIdentifier == null || accountIdentifier.trim().length() < 6) {
            throw new InvalidFileException(
                    "Account identifier is missing or too short: '" + accountIdentifier + "'");
        }
        String bin = accountIdentifier.trim().substring(0, 6);
        return localBins.contains(bin);
    }

    public boolean isBinMasterLoaded() {
        return !localBins.isEmpty();
    }

    /**
     * Converts Excel cell to BIN string.
     * CRITICAL: NUMERIC cells must use (long) cast to avoid "458868.0" or "4.58868E5".
     */
    public String extractBinFromCell(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case STRING  -> cell.getStringCellValue().trim();
            case FORMULA -> {
                CellValue evaluated = cell.getSheet().getWorkbook()
                        .getCreationHelper().createFormulaEvaluator().evaluate(cell);
                yield switch (evaluated.getCellType()) {
                    case NUMERIC -> String.valueOf((long) evaluated.getNumberValue());
                    case STRING  -> evaluated.getStringValue().trim();
                    default -> null;
                };
            }
            default -> null;
        };
    }
}
