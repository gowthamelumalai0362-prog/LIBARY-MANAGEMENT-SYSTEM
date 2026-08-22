package com.library.lms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDto {
    @NotNull(message = "Max borrow limit is required")
    @Min(value = 1, message = "Max borrow limit must be at least 1")
    private Integer maxBorrowLimit;

    @NotNull(message = "Loan duration days is required")
    @Min(value = 1, message = "Loan duration must be at least 1 day")
    private Integer loanDurationDays;

    @NotNull(message = "Daily fine rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily fine rate must be non-negative")
    private BigDecimal dailyFineRate;

    @NotNull(message = "Fine threshold is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fine threshold must be non-negative")
    private BigDecimal fineThreshold;

    @NotNull(message = "Hold window hours is required")
    @Min(value = 1, message = "Hold window hours must be at least 1")
    private Integer holdWindowHours;
}
