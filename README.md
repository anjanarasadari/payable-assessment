This project is a Spring Boot RESTful application designed to process transaction CSV files and generate a transformed output CSV with fee calculations based on BIN lookups.

---

### 1. How to Run the Application

#### Prerequisites
- **Java 17** or higher
- **Maven 3.8+**

#### Steps
1.  **Clone or Unzip the Project:**
    Navigate to the root directory of the project (where `pom.xml` is located).
2.  **Build the Project:**
    Run the following command to compile the code and package it:
    ```bash
    mvn clean install
    ```
3.  **Run the Application:**
    Start the Spring Boot application using:
    ```bash
    mvn spring-boot:run
    ```
    The application will start on `http://localhost:8080` by default.

---

### 2. How to Test the APIs

The system exposes two primary endpoints. You can test them using **Postman** or **cURL**.

#### Step 1: Upload BIN Master
Before processing transactions, you must upload the BIN master Excel file to populate the in-memory lookup.
- **Endpoint:** `POST /api/v1/bin-master/upload`
- **Content-Type:** `multipart/form-data`
- **Parameter Name:** `file` (Select a `.xlsx` file)
- **Example cURL:**
  ```bash
  curl -X POST -F "file=@path/to/bin_master.xlsx" http://localhost:8080/api/v1/bin-master/upload
  ```

#### Step 2: Process Transactions
Upload a transaction CSV to receive the transformed output.
- **Endpoint:** `POST /api/v1/transactions/generate-csv`
- **Content-Type:** `multipart/form-data`
- **Parameter Name:** `file` (Select a `.csv` file)
- **Response:** A downloadable CSV file named `generated_data_set.csv`.
- **Example cURL:**
  ```bash
  curl -X POST -F "file=@path/to/transactions.csv" http://localhost:8080/api/v1/transactions/generate-csv --output result.csv
  ```

---

### 3. Key Assumptions

- **BIN Extraction:** The BIN is assumed to be the **first 6 digits** of the `Account Identifier` field in the transaction CSV.
- **MDR Rates:** 
    - **Local:** 2.2% (if BIN exists in the uploaded master).
    - **Foreign:** 3.5% (if BIN does not exist in the uploaded master).
- **Supported Statuses:** Only transactions with statuses `CAPTURED`, `PARTIALLY CAPTURED`, `REFUNDED`, or `PARTIALLY REFUNDED` are processed. All other statuses are skipped.
- **In-Memory Storage:** The BIN master data is stored in-memory (JVM). If the server restarts, the BIN master must be re-uploaded.
- **CSV Format:** The input CSV is expected to have specific headers such as `Order Status`, `Captured Amount (amount only)`, and `Account Identifier`.

---

### 4. Note on AI Tool Usage and Verification

#### How AI Tools Were Used
- **Architecture Design:** AI was used to generate the initial project structure and define the layered architecture (Controller -> Service -> Repository) following SOLID principles.
- **Boilerplate Reduction:** AI assisted in generating DTO Records, exception classes, and standard Spring Boot configurations.
- **Logic Implementation:** Core business logic for fee calculations and CSV parsing (using OpenCSV/Apache POI) was drafted with AI assistance.
- **Test Generation:** Unit tests for service layers were generated to ensure coverage of edge cases (e.g., missing BINs, invalid file formats).

#### How Outputs Were Verified
- **Code Review:** Every line of generated code was manually reviewed for compliance with the project's development guidelines (e.g., constructor injection, naming conventions).
- **Automated Testing:** Unit tests using JUnit 5 and Mockito were executed to verify that business logic (MDR calculations and status filtering) works as expected.
- **Integration Testing:** API endpoints were manually tested via a build environment to ensure correct file handling and response headers.
- **Validation:** Specific validation logic was added to handle invalid Excel/CSV formats, ensuring the application fails gracefully with meaningful error messages.
