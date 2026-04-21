package com.payable.assessment.controller;

import com.payable.assessment.service.BinMasterService;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BinMasterControllerTest {

    @Autowired MockMvc    mockMvc;
    @Autowired BinMasterService binMasterService;

    private static final String UPLOAD_URL = "/api/v1/bin-master/upload";

    // ── Success ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("200 OK with totalBinsLoaded when valid xlsx is uploaded")
    void upload_validFile_returns200WithCount() throws Exception {
        mockMvc.perform(multipart(UPLOAD_URL).file(buildExcelFile(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("BIN master uploaded successfully"))
                .andExpect(jsonPath("$.totalBinsLoaded").value(2));
    }

    // ── Validation failures ───────────────────────────────────────────────────

    @Test
    @DisplayName("400 Bad Request when no file part is supplied")
    void upload_missingFilePart_returns400() throws Exception {
        mockMvc.perform(multipart(UPLOAD_URL)) // no file attached
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 Bad Request when file has wrong extension (.csv instead of .xlsx)")
    void upload_wrongFileType_returns400() throws Exception {
        MockMultipartFile csv = new MockMultipartFile(
                "file", "bins.csv", "text/csv", "BIN CODE\n458868\n".getBytes());

        mockMvc.perform(multipart(UPLOAD_URL).file(csv))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString(".xlsx")));
    }

    @Test
    @DisplayName("400 Bad Request when file is empty")
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "bins.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        mockMvc.perform(multipart(UPLOAD_URL).file(empty))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MockMultipartFile buildExcelFile(int binCount) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("BIN CODE");
            long[] bins = {458868L, 453213L, 404186L};
            for (int i = 0; i < binCount; i++) {
                sheet.createRow(i + 1).createCell(0).setCellValue(bins[i]);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return new MockMultipartFile("file", "Latest_BIN_List.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baos.toByteArray());
        }
    }
}
