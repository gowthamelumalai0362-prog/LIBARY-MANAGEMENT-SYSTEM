package com.library.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalResponseDto {
    private Long borrowRecordId;
    private LocalDate oldDueDate;
    private LocalDate newDueDate;
    private Integer renewalCount;
    private String message;
}
