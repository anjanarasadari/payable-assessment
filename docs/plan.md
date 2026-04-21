# Implementation Plan: Transaction CSV Processing System

## 1. Package Structure
The application will follow a standard Spring Boot layered architecture as defined in the guidelines:
- `com.payable.assessment.controller`: API endpoints for BIN master and transaction processing.
- `com.payable.assessment.service`: Core business logic for BIN lookup, MDR calculation, and CSV processing.
- `com.payable.assessment.model`: Data models for internal representation (e.g., TransactionRow, OutputRow).
- `com.payable.assessment.dto`: API request/response records (e.g., BinUploadResponse).
- `com.payable.assessment.exception`: Custom exceptions and global exception handler.
- `com.payable.assessment.config`: (Optional) App configurations like MDR rates.
- `com.payable.assessment.repository`: In-memory storage for BIN master data.

## 2. New Classes/Interfaces
### 2.1 Model Layer
- `TransactionRow`: Represents a row from the input CSV file.
- `OutputRow`: Represents a row for the transformed output CSV file.

### 2.2 DTO Layer (Java Records)
- `BinUploadResponse(String status, int recordCount)`: Response for BIN master upload.
- `ErrorResponse(String message, String errorCode)`: Standard error response structure.

### 2.3 Repository Layer
- `BinRepository`: An in-memory repository (using `Set<String>` or `Map<String, BinInfo>`) to store local BINs. No database migration needed as this is non-persistent.

### 2.4 Service Layer
- `BinMasterService`: Logic for parsing Excel/CSV BIN master files and updating `BinRepository`.
- `TransactionService`: Logic for processing transaction CSVs, calculating fees, and generating output rows.
- `CsvService`: Utility for reading/writing CSV files using a library like Apache Commons CSV or OpenCSV.

### 2.5 Controller Layer
- `BinMasterController`: Endpoints for uploading and resetting the BIN master.
- `TransactionController`: Endpoint for processing transaction files.

### 2.6 Exception Layer
- `InvalidFileException`: Thrown for invalid file formats or empty files.
- `GlobalExceptionHandler`: Centralized error handling using `@ControllerAdvice`.

## 3. Database Migration Steps
- **None.** This project uses in-memory storage only. No Liquibase or Flyway needed.

## 4. Business Logic: Fee Calculation
- **Input BIN:** First 6 characters of the `cardNumber`.
- **Classification:**
    - `isLocal = BinRepository.exists(BIN)`
- **Fees:**
    - `mdrRate = isLocal ? CONFIG_LOCAL_MDR : CONFIG_FOREIGN_MDR`
    - `feeAmount = transactionAmount * mdrRate`
    - `netAmount = transactionAmount - feeAmount`

## 5. Technology Stack
- **Dependencies (Maven):**
    - `spring-boot-starter-web`: For REST APIs.
    - `spring-boot-starter-validation`: For API validation.
    - `lombok`: For reducing boilerplate in models.
    - `commons-csv`: For CSV parsing and generation.
    - `poi` / `poi-ooxml`: For Excel file parsing (if BIN master is Excel).
    - `spring-boot-starter-test`: For testing.