package com.payable.assessment.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class TransactionRow {

    @CsvBindByName(column = "Order Status")
    private String orderStatus;

    @CsvBindByName(column = "Order Date")
    private String orderDate;

    @CsvBindByName(column = "Last Updated Date")
    private String lastUpdatedDate;

    @CsvBindByName(column = "Order ID")
    private String orderId;

    @CsvBindByName(column = "Order Amount (amount only)")
    private String orderAmount;

    @CsvBindByName(column = "Captured Amount (amount only)")
    private String capturedAmount;

    @CsvBindByName(column = "Account Identifier")
    private String accountIdentifier;
}
