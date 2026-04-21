# Task List: Transaction CSV Processing System

## 1. Project Initialization
- [ ] Initialize Git repository.
- [ ] Create `.gitignore` for Java/Maven.
- [ ] Initial project commit.

## 2. Infrastructure & Foundations
- [ ] Set up Maven dependencies (`pom.xml`): `spring-boot-starter-web`, `lombok`, `commons-csv`, `poi-ooxml`, `spring-boot-starter-test`.
- [ ] Create package structure: `controller`, `service`, `repository`, `model`, `dto`, `exception`.
- [ ] Implement `GlobalExceptionHandler` and custom exceptions (`InvalidFileException`).

## 3. Repository & Data Layer
- [x] Create `BinRepository` (in-memory storage using `Set<String>`).
- [x] Create `TransactionRow` and `OutputRow` models.
- [x] Refactor `BinRepository` to use interface and in-memory implementation.

## 4. DTO Layer (Java Records)
- [ ] Create `BinUploadResponse` record.
- [ ] Create `ErrorResponse` record.

## 5. Service Layer
- [ ] Implement `CsvService`: Utility for reading/writing CSV files.
- [ ] Implement `BinMasterService`: Logic for parsing Excel/CSV and updating `BinRepository`.
- [ ] Implement `TransactionService`: Core business logic for fee calculation and transformation.

## 6. Controller Layer
- [ ] Implement `BinMasterController`: Endpoints for uploading and resetting BIN master.
- [ ] Implement `TransactionController`: Endpoint for uploading transaction files and downloading processed CSV.

## 7. Testing
- [ ] Write unit tests for `BinMasterService`.
- [ ] Write unit tests for `TransactionService`.
- [ ] Write integration tests for API endpoints.

## 8. Final Verification
- [ ] Manual test: Upload BIN master.
- [ ] Manual test: Process transaction CSV.
- [ ] Verify against Junie Development Guidelines.