# PAYable Backend Developer Assessment

Spring Boot service that accepts a transaction CSV and a BIN master Excel file,
applies MDR business rules, and generates a transformed output CSV.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |

---

## How to run

```bash
# Clone / unzip the project, then:
cd payable-assessment
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

---

## API usage

### Step 1 — Upload BIN master (required first)

```
POST /api/v1/bin-master/upload
Content-Type: multipart/form-data
Field name:   file   (Latest_BIN_List.xlsx)
```

**Example (curl):**
```bash
curl -X POST http://localhost:8080/api/v1/bin-master/upload \
  -F "file=@Latest_BIN_List.xlsx"
```

**Success response (200):**
```json
{
  "message": "BIN master uploaded successfully",
  "totalBinsLoaded": 397
}
```

---

### Step 2 — Generate CSV

```
POST /api/v1/transactions/generate-csv
Content-Type: multipart/form-data
Field name:   file   (sample_data_set.csv)
```

**Example (curl):**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/generate-csv \
  -F "file=@sample_data_set.csv" \
  --output generated_data_set.csv
```

**Response:**
- Body: downloadable `generated_data_set.csv`
- `X-Rows-Processed` header: count of successfully transformed rows
- `X-Rows-Skipped` header: count of skipped rows
- `X-Skipped-Details` header: reason per skipped row (pipe-separated)

---

## Running tests

```bash
mvn test
```

Test coverage includes:
- BIN master parsing (integer-to-string conversion)
- Local vs foreign BIN detection
- MDR rule application (2.2% local, 3.5% foreign)
- Fee calculation with `RoundingMode.HALF_UP`
- Net amount = gross − rounded fee
- Row type mapping for all 4 statuses including PARTIALLY CAPTURED / PARTIALLY REFUNDED
- Case-insensitive status matching
- CSV transformation output (header + data rows)
- API success and validation failure scenarios (MockMvc)
- Unsupported Order Status row skipping

---

## Postman collection

Import `PAYable_Assessment.postman_collection.json` into Postman.

The collection contains:
- Upload BIN Master (success + 2 validation failure cases)
- Generate CSV (success + 2 validation failure cases)

The `{{baseUrl}}` variable defaults to `http://localhost:8080`.
To change it: go to the collection → Variables tab → update the value.

**Workflow:** Always run "Upload BIN Master" before "Generate CSV".
The BIN master is stored in memory and resets when the server restarts.

---

## Business rules summary

| Rule | Detail |
|---|---|
| Local BIN MDR | 2.2% — BIN found in column A of BIN master Excel |
| Foreign BIN MDR | 3.5% — BIN not found in BIN master |
| BIN extraction | First 6 characters of Account Identifier |
| Fee rounding | `BigDecimal` with `RoundingMode.HALF_UP`, 2 decimal places |
| Net amount | `GROSS_LOCAL_AMOUNT − FEE_LOCAL_AMOUNT` (fee rounded first) |
| Currency | Hardcoded `LKR` |
| CAPTURED → | `PAYMENT` |
| PARTIALLY CAPTURED → | `PAYMENT` |
| REFUNDED → | `REFUND` |
| PARTIALLY REFUNDED → | `REFUND` |
| Other statuses | Skipped, reported in response headers |
| Status matching | Case-insensitive |
| Amount input | Commas stripped before parsing (e.g. `21,160.00` → `21160.00`) |

---

## Assumptions

1. **BIN master is in-memory** — it resets on server restart. No database is used per the scope boundaries in the brief.
2. **BIN master must be uploaded before calling Generate CSV** — the service returns HTTP 412 if the BIN list is empty.
3. **The "Captured Amount (amount only)" column** is used as `GROSS_LOCAL_AMOUNT`, not "Order Amount (amount only)". This matches the spec: "Use Captured Amount" for GROSS.
4. **Rows with invalid amounts or missing account identifiers** are skipped with a reason reported in `X-Skipped-Details`, consistent with the unsupported-status skip behaviour.
5. **Output amounts** always have exactly 2 decimal places (e.g. `465.52`, not `465.5200`).

---

## AI tool usage note

Claude (Anthropic) was used to assist with:
- Scaffolding the Spring Boot project structure and boilerplate
- Generating unit test cases covering edge cases from the spec
- Drafting the Postman collection JSON structure

All generated code was reviewed and verified against:
- The actual `sample_data_set.csv` column names and data format
- The actual `Latest_BIN_List.xlsx` structure (BIN CODE stored as integer)
- Manual calculation checks for MDR fee and net amounts
- The assessment brief business rules (MDR %, rounding mode, row type mapping)

The critical BIN integer-to-string conversion, `BigDecimal` rounding behaviour,
and case-insensitive status matching were specifically verified against the files provided.
