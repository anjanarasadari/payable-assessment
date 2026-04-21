package com.payable.assessment.service;

import com.payable.assessment.exception.InvalidFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;

class BinMasterServiceTest {

    private BinMasterService service;

    @BeforeEach
    void setUp() {
        service = new BinMasterService();
    }

    // ── loadBins ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Loads BINs successfully from a valid Excel file")
    void loadBins_validFile_returnsTotalCount() throws Exception {
        MockMultipartFile file = buildExcelFile(new Object[][]{
                {"BIN CODE", "BANK", "CARD TYPE"},
                {458868L, "BANK A", "VISA"},
                {453213L, "BANK B", "MASTER"},
        });

        int count = service.loadBins(file);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Integer BIN codes are stored as Strings (critical conversion)")
    void loadBins_integerBinStoredAsString() throws Exception {
        MockMultipartFile file = buildExcelFile(new Object[][]{
                {"BIN CODE"},
                {458868L},
        });

        service.loadBins(file);

        // Must be found as string "458868", not integer
        assertThat(service.isLocalBin("458868xxxxxx1234")).isTrue();
    }

    @Test
    @DisplayName("Returns true for a BIN that exists in the master list")
    void isLocalBin_binExists_returnsTrue() throws Exception {
        loadSingleBin(469176L);
        assertThat(service.isLocalBin("469176xxxxxx0001")).isTrue();
    }

    @Test
    @DisplayName("Returns false for a BIN that does not exist in the master list (foreign BIN)")
    void isLocalBin_binNotExists_returnsFalse() throws Exception {
        loadSingleBin(469176L);
        assertThat(service.isLocalBin("999999xxxxxx0001")).isFalse();
    }

    @Test
    @DisplayName("isBinMasterLoaded returns false before any upload")
    void isBinMasterLoaded_beforeUpload_returnsFalse() {
        assertThat(service.isBinMasterLoaded()).isFalse();
    }

    @Test
    @DisplayName("isBinMasterLoaded returns true after successful upload")
    void isBinMasterLoaded_afterUpload_returnsTrue() throws Exception {
        loadSingleBin(458868L);
        assertThat(service.isBinMasterLoaded()).isTrue();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Throws when file is null")
    void loadBins_nullFile_throwsInvalidFileException() {
        assertThatThrownBy(() -> service.loadBins(null))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("missing or empty");
    }

    @Test
    @DisplayName("Throws when file is empty")
    void loadBins_emptyFile_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile("file", "bins.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);
        assertThatThrownBy(() -> service.loadBins(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("missing or empty");
    }

    @Test
    @DisplayName("Throws when file has wrong extension")
    void loadBins_wrongFileType_throwsInvalidFileException() {
        MockMultipartFile file = new MockMultipartFile("file", "bins.csv",
                "text/csv", "BIN CODE\n458868\n".getBytes());
        assertThatThrownBy(() -> service.loadBins(file))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining(".xlsx");
    }

    @Test
    @DisplayName("Throws when account identifier is shorter than 6 characters")
    void isLocalBin_shortIdentifier_throwsInvalidFileException() throws Exception {
        loadSingleBin(458868L);
        assertThatThrownBy(() -> service.isLocalBin("1234"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("Throws when account identifier is null")
    void isLocalBin_nullIdentifier_throwsInvalidFileException() throws Exception {
        loadSingleBin(458868L);
        assertThatThrownBy(() -> service.isLocalBin(null))
                .isInstanceOf(InvalidFileException.class);
    }

    // ── extractBinFromCell ────────────────────────────────────────────────────

    @Test
    @DisplayName("extractBinFromCell converts NUMERIC cell to String without scientific notation")
    void extractBinFromCell_numericCell_returnsString() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue(458868.0);

            String result = service.extractBinFromCell(cell);

            assertThat(result).isEqualTo("458868");
        }
    }

    @Test
    @DisplayName("extractBinFromCell handles STRING cell")
    void extractBinFromCell_stringCell_returnsString() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("458868");

            String result = service.extractBinFromCell(cell);

            assertThat(result).isEqualTo("458868");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadSingleBin(long binValue) throws Exception {
        MockMultipartFile file = buildExcelFile(new Object[][]{
                {"BIN CODE"},
                {binValue},
        });
        service.loadBins(file);
    }

    /**
     * Builds an in-memory .xlsx file from a 2D array where the first row is the header.
     * Long values are written as NUMERIC cells (matching real Excel behaviour).
     */
    private MockMultipartFile buildExcelFile(Object[][] data) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = data[r][c];
                    if (val instanceof Long)   cell.setCellValue((Long) val);
                    else if (val instanceof String) cell.setCellValue((String) val);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", "Latest_BIN_List.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        }
    }
}
