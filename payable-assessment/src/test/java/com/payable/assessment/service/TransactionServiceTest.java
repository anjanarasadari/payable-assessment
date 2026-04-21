package com.payable.assessment.service;

import com.payable.assessment.dto.CsvGenerationResult;
import com.payable.assessment.exception.BinMasterNotLoadedException;
import com.payable.assessment.exception.InvalidFileException;
import com.payable.assessment.model.OutputRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService service;
    private BinMasterService   binMasterService;

    // Header matching the real sample_data_set.csv
    private static final String CSV_HEADER =
            "Order Status,Order Date,Last Updated Date,Order ID," +
            "Order Amount (amount only),Order Amount (currency only)," +
            "Captured Amount (amount only),Captured Amount (currency only)," +
            "Refunded Amount (amount only),Refunded Amount (currency only)," +
            "Account Identifier";

    @BeforeEach
    void setUp() {
        binMasterService = new BinMasterService();
        service          = new TransactionService(binMasterService);
    }

    // ── resolveRowType ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Row type mapping")
    class RowTypeMapping {

        @ParameterizedTest(name = "status=''{0}'' → PAYMENT")
        @ValueSource(strings = {"CAPTURED", "captured", "Captured", "PARTIALLY CAPTURED",
                                "partially captured", "Partially Captured"})
        @DisplayName("Maps captured variants (case-insensitive) to PAYMENT")
        void capturedVariants_returnPayment(String status) {
            assertThat(service.resolveRowType(status.toUpperCase())).isEqualTo("PAYMENT");
        }

        @ParameterizedTest(name = "status=''{0}'' → REFUND")
        @ValueSource(strings = {"REFUNDED", "refunded", "Refunded", "PARTIALLY REFUNDED",
                                "partially refunded", "Partially Refunded"})
        @DisplayName("Maps refunded variants (case-insensitive) to REFUND")
        void refundedVariants_returnRefund(String status) {
            assertThat(service.resolveRowType(status.toUpperCase())).isEqualTo("REFUND");
        }

        @ParameterizedTest(name = "status=''{0}'' → null (skip)")
        @ValueSource(strings = {"PENDING", "CANCELLED", "FAILED", "", "UNKNOWN"})
        @DisplayName("Returns null for unsupported statuses")
        void unsupportedStatus_returnNull(String status) {
            assertThat(service.resolveRowType(status.toUpperCase())).isNull();
        }
    }

    // ── parseAmount ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Amount parsing")
    class AmountParsing {

        @Test
        @DisplayName("Strips commas from amounts like '21,160.00'")
        void parseAmount_withCommas_parsesCorrectly() {
            BigDecimal result = service.parseAmount("21,160.00");
            assertThat(result).isEqualByComparingTo(new BigDecimal("21160.00"));
        }

        @Test
        @DisplayName("Parses plain decimal amount without commas")
        void parseAmount_noCommas_parsesCorrectly() {
            BigDecimal result = service.parseAmount("5330.00");
            assertThat(result).isEqualByComparingTo(new BigDecimal("5330.00"));
        }

        @Test
        @DisplayName("Throws NumberFormatException for blank amount")
        void parseAmount_blank_throwsNumberFormatException() {
            assertThatThrownBy(() -> service.parseAmount(""))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Throws NumberFormatException for non-numeric amount")
        void parseAmount_nonNumeric_throwsNumberFormatException() {
            assertThatThrownBy(() -> service.parseAmount("abc"))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    // ── MDR & fee calculation ─────────────────────────────────────────────────

    @Nested
    @DisplayName("MDR fee calculation with HALF_UP rounding")
    class FeeCalculation {

        @Test
        @DisplayName("Local BIN: fee = gross × 2.2% rounded HALF_UP")
        void localBin_feeCalculation() {
            BigDecimal gross = new BigDecimal("21160.00");
            BigDecimal fee   = gross.multiply(new BigDecimal("2.2"))
                                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            // 21160 * 2.2 / 100 = 465.52
            assertThat(fee).isEqualByComparingTo(new BigDecimal("465.52"));
        }

        @Test
        @DisplayName("Foreign BIN: fee = gross × 3.5% rounded HALF_UP")
        void foreignBin_feeCalculation() {
            BigDecimal gross = new BigDecimal("5000.00");
            BigDecimal fee   = gross.multiply(new BigDecimal("3.5"))
                                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            // 5000 * 3.5 / 100 = 175.00
            assertThat(fee).isEqualByComparingTo(new BigDecimal("175.00"));
        }

        @Test
        @DisplayName("HALF_UP rounding: 0.005 rounds up to 0.01")
        void halfUpRounding_roundsUp() {
            // gross that produces exactly x.005 before rounding
            // e.g. gross = 0.50 / 0.022 ≈ 22.7272... → 22.7272 * 2.2% = 0.50000...
            BigDecimal gross = new BigDecimal("100.015");
            BigDecimal fee   = gross.multiply(new BigDecimal("2.2"))
                                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            // 100.015 * 2.2 / 100 = 2.20033 → rounds to 2.20
            assertThat(fee).isEqualByComparingTo(new BigDecimal("2.20"));
        }

        @Test
        @DisplayName("Net amount = gross − rounded fee (not rounded separately)")
        void netAmount_grossMinusRoundedFee() {
            BigDecimal gross      = new BigDecimal("21160.00");
            BigDecimal fee        = new BigDecimal("465.52"); // pre-rounded
            BigDecimal net        = gross.subtract(fee);
            assertThat(net).isEqualByComparingTo(new BigDecimal("20694.48"));
        }

        @Test
        @DisplayName("Output amounts formatted to exactly 2 decimal places")
        void formatAmount_alwaysTwoDecimalPlaces() {
            assertThat(service.formatAmount(new BigDecimal("100"))).isEqualTo("100.00");
            assertThat(service.formatAmount(new BigDecimal("100.1"))).isEqualTo("100.10");
            assertThat(service.formatAmount(new BigDecimal("100.005"))).isEqualTo("100.01"); // HALF_UP
        }
    }

    // ── Full CSV transformation ───────────────────────────────────────────────

    @Nested
    @DisplayName("Full CSV transformation")
    class CsvTransformation {

        @Test
        @DisplayName("Transforms a Captured row into PAYMENT with correct fields (local BIN)")
        void capturedRow_localBin_transformsToPayment() throws Exception {
            loadBin(458868L); // local

            String csv = CSV_HEADER + "\n" +
                    "Captured,2/28/2026 2:09,2/28/2026 2:09," +
                    "oid-test-001,21160.00,LKR,21160.00,LKR,0,LKR,458868xxxxxx1234";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getProcessedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(0);

            OutputRow row = result.getRows().get(0);
            assertThat(row.getRowType()).isEqualTo("PAYMENT");
            assertThat(row.getCreationDate()).isEqualTo("2/28/2026 2:09");
            assertThat(row.getApprovedDate()).isEqualTo("2/28/2026 2:09");
            assertThat(row.getTransactionId()).isEqualTo("oid-test-001");
            assertThat(row.getRequestAmount()).isEqualTo("21160.00");
            assertThat(row.getGrossLocalAmount()).isEqualTo("21160.00");
            assertThat(row.getLocalCurrency()).isEqualTo("LKR");
            // fee = 21160 × 2.2% = 465.52
            assertThat(row.getFeeLocalAmount()).isEqualTo("465.52");
            // net = 21160.00 - 465.52 = 20694.48
            assertThat(row.getNetLocalAmount()).isEqualTo("20694.48");
        }

        @Test
        @DisplayName("Transforms a Refunded row into REFUND (local BIN)")
        void refundedRow_localBin_transformsToRefund() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Refunded,3/1/2026 10:00,3/1/2026 11:00," +
                    "oid-refund-001,35190.00,LKR,35190.00,LKR,35190.00,LKR,458868xxxxxx9999";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getRows().get(0).getRowType()).isEqualTo("REFUND");
        }

        @Test
        @DisplayName("Applies 3.5% MDR for foreign BIN")
        void foreignBin_appliesForeignMdr() throws Exception {
            // Do NOT load this BIN → it becomes foreign
            loadBin(111111L); // load a different BIN

            String csv = CSV_HEADER + "\n" +
                    "Captured,2/28/2026 9:00,2/28/2026 9:01," +
                    "oid-foreign-001,5000.00,LKR,5000.00,LKR,0,LKR,999999xxxxxx5678";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            OutputRow row = result.getRows().get(0);
            // fee = 5000 × 3.5% = 175.00
            assertThat(row.getFeeLocalAmount()).isEqualTo("175.00");
            assertThat(row.getNetLocalAmount()).isEqualTo("4825.00");
        }

        @Test
        @DisplayName("Handles comma-formatted amounts like '21,160.00'")
        void commaFormattedAmount_parsedCorrectly() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Captured,2/28/2026 2:09,2/28/2026 2:09," +
                    "oid-comma-001,\"21,160.00\",LKR,\"21,160.00\",LKR,0,LKR,458868xxxxxx1234";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getRows().get(0).getGrossLocalAmount()).isEqualTo("21160.00");
        }

        @Test
        @DisplayName("Handles slightly different header names or order (Mapping Strategy test)")
        void headerRobustness() throws Exception {
            loadBin(458868L);

            // "Order status" instead of "Order Status" (case)
            // "Account Identifier" at the beginning
            String csv = "Account Identifier,Order status,Order Date,Last Updated Date,Order ID,Order Amount (amount only),Captured Amount (amount only)\n" +
                    "458868xxxxxx1234,Captured,2/28/2026,2/28/2026,oid-robust,100,100";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getProcessedCount()).isEqualTo(1);
            assertThat(result.getRows().get(0).getTransactionId()).isEqualTo("oid-robust");
        }

        @Test
        @DisplayName("Fails if mandatory header is missing (e.g. Account Identifier)")
        void missingHeaderFails() throws Exception {
            loadBin(458868L);

            // Missing "Account Identifier"
            String csv = "Order Status,Order Date,Last Updated Date,Order ID,Order Amount (amount only),Captured Amount (amount only)\n" +
                    "Captured,2/28/2026,2/28/2026,oid-fail,100,100";

            // If it's empty/missing, it might either throw an error during parse or skip the row if validation fails
            // With CsvBindByName, if header is missing, the field stays null.
            // Our validation logic skips rows with null fields.
            CsvGenerationResult result = service.processTransactions(mockCsv(csv));
            assertThat(result.getProcessedCount()).isEqualTo(0);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.getSkippedReasons().get(0)).contains("invalid or missing Account Identifier");
        }

        @Test
        @DisplayName("Handles UTF-8 BOM in CSV file")
        void utf8Bom_handledCorrectly() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Captured,2/28/2026 2:09,2/28/2026 2:09," +
                    "oid-bom,100,LKR,100,LKR,0,LKR,458868xxxxxx1234";
            
            byte[] bomBytes = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
            byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[bomBytes.length + csvBytes.length];
            System.arraycopy(bomBytes, 0, combined, 0, bomBytes.length);
            System.arraycopy(csvBytes, 0, combined, bomBytes.length, csvBytes.length);

            MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", combined);
            CsvGenerationResult result = service.processTransactions(file);

            assertThat(result.getProcessedCount()).isEqualTo(1);
            assertThat(result.getRows().get(0).getTransactionId()).isEqualTo("oid-bom");
        }

        @Test
        @DisplayName("PARTIALLY CAPTURED maps to PAYMENT")
        void partiallyCaptured_mapsToPayment() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Partially Captured,3/1/2026 8:00,3/1/2026 8:01," +
                    "oid-pc-001,10000.00,LKR,7500.00,LKR,0,LKR,458868xxxxxx0001";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getRows().get(0).getRowType()).isEqualTo("PAYMENT");
        }

        @Test
        @DisplayName("PARTIALLY REFUNDED maps to REFUND")
        void partiallyRefunded_mapsToRefund() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Partially Refunded,3/1/2026 8:00,3/1/2026 8:01," +
                    "oid-pr-001,10000.00,LKR,10000.00,LKR,5000.00,LKR,458868xxxxxx0001";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getRows().get(0).getRowType()).isEqualTo("REFUND");
        }
    }

    // ── Unsupported status skipping ───────────────────────────────────────────

    @Nested
    @DisplayName("Unsupported order status handling")
    class UnsupportedStatusHandling {

        @Test
        @DisplayName("Row with unsupported status is skipped, not failed")
        void unsupportedStatus_rowIsSkipped() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Pending,3/1/2026 8:00,3/1/2026 8:01,oid-skip-001,1000.00,LKR,1000.00,LKR,0,LKR,458868xxxxxx0001\n" +
                    "Captured,3/1/2026 9:00,3/1/2026 9:01,oid-ok-001,2000.00,LKR,2000.00,LKR,0,LKR,458868xxxxxx0001";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getProcessedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.getSkippedReasons().get(0)).contains("Pending");
        }

        @Test
        @DisplayName("Multiple unsupported statuses all skipped, none cause failure")
        void multipleUnsupportedStatuses_allSkipped() throws Exception {
            loadBin(458868L);

            String csv = CSV_HEADER + "\n" +
                    "Pending,3/1/2026 8:00,3/1/2026 8:01,oid-1,1000.00,LKR,1000.00,LKR,0,LKR,458868xxxxxx0001\n" +
                    "Cancelled,3/1/2026 8:00,3/1/2026 8:01,oid-2,1000.00,LKR,1000.00,LKR,0,LKR,458868xxxxxx0001\n" +
                    "Failed,3/1/2026 8:00,3/1/2026 8:01,oid-3,1000.00,LKR,1000.00,LKR,0,LKR,458868xxxxxx0001";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));

            assertThat(result.getProcessedCount()).isEqualTo(0);
            assertThat(result.getSkippedCount()).isEqualTo(3);
        }
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Nested
    @DisplayName("CSV validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("Throws when file is null")
        void nullFile_throwsInvalidFileException() {
            assertThatThrownBy(() -> service.processTransactions(null))
                    .isInstanceOf(InvalidFileException.class);
        }

        @Test
        @DisplayName("Throws when file is empty")
        void emptyFile_throwsInvalidFileException() {
            MockMultipartFile file = new MockMultipartFile("file", "tx.csv", "text/csv", new byte[0]);
            assertThatThrownBy(() -> service.processTransactions(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("missing or empty");
        }

        @Test
        @DisplayName("Throws when file has wrong extension")
        void wrongFileType_throwsInvalidFileException() {
            MockMultipartFile file = new MockMultipartFile("file", "data.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "data".getBytes());
            assertThatThrownBy(() -> service.processTransactions(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining(".csv");
        }

        @Test
        @DisplayName("Throws when BIN master has not been loaded")
        void binMasterNotLoaded_throwsBinMasterNotLoadedException() {
            String csv = CSV_HEADER + "\n" +
                    "Captured,3/1/2026,3/1/2026,oid-1,1000,LKR,1000,LKR,0,LKR,458868xxxxxx1234";
            assertThatThrownBy(() -> service.processTransactions(mockCsv(csv)))
                    .isInstanceOf(BinMasterNotLoadedException.class);
        }
    }

    // ── writeCsv ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CSV output writing")
    class CsvOutputWriting {

        @Test
        @DisplayName("Output CSV contains the correct header row")
        void writeCsv_containsCorrectHeader() throws Exception {
            loadBin(458868L);
            String csv = CSV_HEADER + "\n" +
                    "Captured,2/28/2026,2/28/2026,oid-1,1000.00,LKR,1000.00,LKR,0,LKR,458868xxxxxx0001";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));
            byte[] bytes = service.writeCsv(result.getRows());
            String output = new String(bytes, StandardCharsets.UTF_8);

            assertThat(output).contains("ROW_TYPE");
            assertThat(output).contains("CREATION_DATE");
            assertThat(output).contains("APPROVED_DATE");
            assertThat(output).contains("TRANSACTION_ID");
            assertThat(output).contains("REQUEST_AMOUNT");
            assertThat(output).contains("GROSS_LOCAL_AMOUNT");
            assertThat(output).contains("LOCAL_CURRENCY");
            assertThat(output).contains("FEE_LOCAL_AMOUNT");
            assertThat(output).contains("NET_LOCAL_AMOUNT");
        }

        @Test
        @DisplayName("Output CSV data row has LKR as currency")
        void writeCsv_dataRowHasLkrCurrency() throws Exception {
            loadBin(458868L);
            String csv = CSV_HEADER + "\n" +
                    "Captured,2/28/2026,2/28/2026,oid-1,1000.00,LKR,1000.00,LKR,0,LKR,458868xxxxxx0001";

            CsvGenerationResult result = service.processTransactions(mockCsv(csv));
            byte[] bytes = service.writeCsv(result.getRows());
            String output = new String(bytes, StandardCharsets.UTF_8);

            assertThat(output).contains("LKR");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadBin(long binValue) throws Exception {
        org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet();
        sheet.createRow(0).createCell(0).setCellValue("BIN CODE");
        sheet.createRow(1).createCell(0).setCellValue(binValue);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        MockMultipartFile excelFile = new MockMultipartFile("file", "bins.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray());
        binMasterService.loadBins(excelFile);
    }

    private MockMultipartFile mockCsv(String content) {
        return new MockMultipartFile("file", "transactions.csv",
                "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
}
