package com.library.lms.repository;

import com.library.lms.entity.BookCopy;
import com.library.lms.entity.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {
    Optional<BookCopy> findByBarcode(String barcode);
    boolean existsByBarcode(String barcode);
    List<BookCopy> findByBookId(Long bookId);
    List<BookCopy> findByBookIdAndStatus(Long bookId, CopyStatus status);
    long countByBookIdAndStatus(Long bookId, CopyStatus status);
    long countByStatus(CopyStatus status);
}
