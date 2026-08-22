package com.library.lms.repository;

import com.library.lms.entity.Book;
import com.library.lms.entity.BookCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE " +
           "(:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.publisher) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR b.category = :category) AND " +
           "(:availableOnly IS NULL OR :availableOnly = false OR b.availableCopies > 0)")
    Page<Book> searchBooks(@Param("search") String search,
                           @Param("category") BookCategory category,
                           @Param("availableOnly") Boolean availableOnly,
                           Pageable pageable);

    @Query("SELECT SUM(b.totalCopies) FROM Book b")
    Long countTotalBookCopies();

    @Query("SELECT SUM(b.availableCopies) FROM Book b")
    Long countTotalAvailableCopies();
}
