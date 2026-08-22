package com.library.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsDto {
    private long totalBooks;
    private long totalCopies;
    private long availableCopies;
    private long borrowedCopies;
    private long reservedCopies;
    private long damagedLostCopies;

    private long totalMembers;
    private long activeBorrowersCount;
    private long activeLoans;
    private long overdueLoans;
    private long pendingReservations;
    private long readyForPickupReservations;

    private BigDecimal totalCollectedRevenue;
    private BigDecimal totalPendingFines;
    private long unpaidFinesCount;

    private List<TopBookDto> topBorrowedBooks;
    private Map<String, Long> categoryDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopBookDto {
        private Long bookId;
        private String title;
        private String author;
        private String coverImageUrl;
        private long borrowCount;
    }
}
