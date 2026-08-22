package com.library.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinePaymentDto {
    private String paymentMethod; // e.g. "CREDIT_CARD", "DEBIT_CARD", "UPI", "CASH", "CAMPUS_CARD"
    private String notes;
}
