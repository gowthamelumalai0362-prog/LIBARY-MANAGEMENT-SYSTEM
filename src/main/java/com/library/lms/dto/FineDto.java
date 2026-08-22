package com.library.lms.dto;

import com.library.lms.entity.FineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FineDto {
    private Long id;
    private Long borrowRecordId;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String bookTitle;
    private String barcode;
    private BigDecimal amount;
    private FineStatus status;
    private String reason;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private String waivedReason;
    private LocalDateTime createdAt;
}
