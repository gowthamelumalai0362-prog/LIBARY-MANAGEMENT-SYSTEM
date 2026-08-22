package com.library.lms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequestDto {
    @NotNull(message = "Book ID is required")
    private Long bookId;

    private Long userId; // Optional for member themselves, specified if librarian creates on behalf
}
