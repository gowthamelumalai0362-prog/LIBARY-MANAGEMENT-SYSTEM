package com.library.lms.dto;

import com.library.lms.entity.CopyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCopyStatusUpdateDto {
    @NotNull(message = "Status is required")
    private CopyStatus status;
    private String conditionNotes;
}
