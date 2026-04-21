# Requirements: Transaction CSV Processing System

## 1. Purpose
Build a Spring Boot REST backend that processes transaction CSV files and generates a transformed output CSV with fee calculations based on BIN lookups.

## 2. Functional Requirements
### 2.1 BIN Master Management
- Support uploading a "BIN master" file (Excel/CSV) to populate an in-memory database of local BINs.
- **In-Memory Storage:** No persistent database (e.g., MySQL, H2). The data resides in memory and resets on server restart.
- **Lookup Logic:**
    - If a transaction's BIN (first 6 digits of the card number) is found in the BIN master, it is classified as **Local**.
    - If not found, it is classified as **Foreign**.

### 2.2 Transaction Processing
- **Input:** A CSV file containing transaction data.
- **Transformation:**
    - Determine if the transaction is Local or Foreign.
    - Apply MDR (Merchant Discount Rate) calculations based on the classification.
    - Calculate net amount and fees.
- **Output:** Generate a transformed CSV file for download.

### 2.3 Business Logic (MDR Rates)
- **Local Transactions:** Apply Local MDR rate.
- **Foreign Transactions:** Apply Foreign MDR rate.
- *Specific rate values to be defined in configuration or code.*

## 3. Non-Functional Requirements
- **Technology Stack:** Java 17, Spring Boot 3.3.x, Maven.
- **Architecture:** Layered architecture (Controller, Service, Model/DTO).
- **Scope Limits:**
    - No Frontend.
    - No Authentication/Authorization.
    - No Email notifications.
    - No persistent database.

## 4. API Endpoints
- `POST /api/v1/bin-master/upload`: Upload BIN master file.
- `POST /api/v1/transactions/process`: Upload transaction CSV and receive processed CSV.

## 5. Constraints
- Must follow Junie Development Guidelines.
- Use Records for DTOs.
- Constructor injection for dependencies.
- Global exception handling for file validation and processing errors.