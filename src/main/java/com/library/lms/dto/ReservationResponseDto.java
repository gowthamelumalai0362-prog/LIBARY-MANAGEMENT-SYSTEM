package com.library.lms.dto;

import com.library.lms.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDto {
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String coverImageUrl;
    private Long heldCopyId;
    private String heldCopyBarcode;
    private LocalDateTime reservationDate;
    private ReservationStatus status;
    private LocalDateTime expiryDate;
    private Integer queuePosition;
    private boolean isExpired;
}
