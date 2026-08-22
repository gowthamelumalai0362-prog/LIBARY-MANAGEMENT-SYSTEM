package com.library.lms.repository;

import com.library.lms.entity.Fine;
import com.library.lms.entity.FineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    Page<Fine> findByUserId(Long userId, Pageable pageable);
    List<Fine> findByUserIdAndStatus(Long userId, FineStatus status);
    Page<Fine> findByStatus(FineStatus status, Pageable pageable);
    Optional<Fine> findByBorrowRecordId(Long borrowRecordId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.user.id = :userId AND f.status = 'UNPAID'")
    BigDecimal sumUnpaidFinesByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.status = 'PAID'")
    BigDecimal sumTotalCollectedRevenue();

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.status = 'UNPAID'")
    BigDecimal sumTotalPendingFines();

    long countByStatus(FineStatus status);
}
