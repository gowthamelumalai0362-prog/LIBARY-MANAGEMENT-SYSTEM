package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "library_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "max_borrow_limit", nullable = false)
    @Builder.Default
    private Integer maxBorrowLimit = 5;

    @Column(name = "loan_duration_days", nullable = false)
    @Builder.Default
    private Integer loanDurationDays = 14;

    @Column(name = "daily_fine_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal dailyFineRate = new BigDecimal("0.50");

    @Column(name = "fine_threshold", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fineThreshold = new BigDecimal("10.00");

    @Column(name = "hold_window_hours", nullable = false)
    @Builder.Default
    private Integer holdWindowHours = 48;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
