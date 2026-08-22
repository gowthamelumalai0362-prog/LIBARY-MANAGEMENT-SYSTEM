package com.library.lms.dto;

import com.library.lms.entity.BorrowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecordDto {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String coverImageUrl;
    private Long bookCopyId;
    private String barcode;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private BorrowStatus status;
    private Integer renewalCount;
    private long daysOverdue;
    private BigDecimal currentFine;
    private boolean isOverdue;
}
