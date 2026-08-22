package com.library.lms.repository;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.BorrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    Page<BorrowRecord> findByUserId(Long userId, Pageable pageable);
    List<BorrowRecord> findByUserIdAndStatus(Long userId, BorrowStatus status);
    long countByUserIdAndStatus(Long userId, BorrowStatus status);

    Optional<BorrowRecord> findByBookCopyIdAndStatus(Long bookCopyId, BorrowStatus status);
    List<BorrowRecord> findByBookCopyId(Long bookCopyId);

    Page<BorrowRecord> findByStatus(BorrowStatus status, Pageable pageable);
    long countByStatus(BorrowStatus status);

    List<BorrowRecord> findByStatusAndDueDateBefore(BorrowStatus status, LocalDate date);
    List<BorrowRecord> findByStatusAndDueDate(BorrowStatus status, LocalDate date);

    @Query("SELECT r.bookCopy.book.id, r.bookCopy.book.title, r.bookCopy.book.author, r.bookCopy.book.coverImageUrl, COUNT(r) " +
           "FROM BorrowRecord r GROUP BY r.bookCopy.book.id, r.bookCopy.book.title, r.bookCopy.book.author, r.bookCopy.book.coverImageUrl " +
           "ORDER BY COUNT(r) DESC")
    List<Object[]> findTopBorrowedBooks(Pageable pageable);

    @Query("SELECT COUNT(DISTINCT r.user.id) FROM BorrowRecord r WHERE r.status = 'ACTIVE'")
    long countActiveBorrowers();
}
