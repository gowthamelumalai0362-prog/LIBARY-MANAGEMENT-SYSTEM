package com.library.lms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRequestDto {
    private Long userId; // Optional for member themselves, required if librarian is issuing on behalf of a user

    @NotBlank(message = "Book copy barcode is required")
    private String barcode;
}
