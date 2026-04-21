package com.payable.assessment.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutputRow {

    @CsvBindByPosition(position = 0)
    private String rowType;

    @CsvBindByPosition(position = 1)
    private String creationDate;

    @CsvBindByPosition(position = 2)
    private String approvedDate;

    @CsvBindByPosition(position = 3)
    private String transactionId;

    @CsvBindByPosition(position = 4)
    private String requestAmount;

    @CsvBindByPosition(position = 5)
    private String grossLocalAmount;

    @CsvBindByPosition(position = 6)
    private String localCurrency;

    @CsvBindByPosition(position = 7)
    private String feeLocalAmount;

    @CsvBindByPosition(position = 8)
    private String netLocalAmount;
}
