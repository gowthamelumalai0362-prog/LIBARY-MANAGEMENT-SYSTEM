package com.library.lms.dto;

import com.library.lms.entity.CopyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCopyDto {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String barcode;
    private CopyStatus status;
    private String conditionNotes;
    private LocalDate acquiredDate;
}
