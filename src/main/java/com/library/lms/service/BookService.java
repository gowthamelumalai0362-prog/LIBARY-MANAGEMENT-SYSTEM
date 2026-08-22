package com.library.lms.service;

import com.library.lms.dto.*;
import com.library.lms.entity.*;
import com.library.lms.exception.LibraryBusinessException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookCopyRepository;
import com.library.lms.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;

    @Transactional(readOnly = true)
    public Page<BookResponseDto> searchBooks(String search, BookCategory category, Boolean availableOnly, Pageable pageable) {
        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Book> bookPage = bookRepository.searchBooks(query, category, availableOnly, pageable);
        return bookPage.map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public BookResponseDto getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
        return toResponseDto(book);
    }

    @Transactional
    public BookResponseDto createBook(BookCreateDto dto) {
        if (bookRepository.existsByIsbn(dto.getIsbn().trim())) {
            throw new LibraryBusinessException("Book with ISBN " + dto.getIsbn() + " already exists");
        }

        Book book = Book.builder()
                .title(dto.getTitle().trim())
                .author(dto.getAuthor().trim())
                .isbn(dto.getIsbn().trim())
                .category(dto.getCategory())
                .publisher(dto.getPublisher())
                .publicationYear(dto.getPublicationYear())
                .description(dto.getDescription())
                .coverImageUrl(dto.getCoverImageUrl())
                .copies(new ArrayList<>())
                .build();

        Book savedBook = bookRepository.save(book);

        int copiesToCreate = dto.getInitialCopiesCount() != null ? dto.getInitialCopiesCount() : 1;
        for (int i = 1; i <= copiesToCreate; i++) {
            String barcode = generateBarcode(savedBook.getIsbn(), i);
            BookCopy copy = BookCopy.builder()
                    .book(savedBook)
                    .barcode(barcode)
                    .status(CopyStatus.AVAILABLE)
                    .acquiredDate(LocalDate.now())
                    .conditionNotes("Initial stock")
                    .build();
            savedBook.addCopy(copy);
        }

        savedBook = bookRepository.save(savedBook);
        return toResponseDto(savedBook);
    }

    @Transactional
    public BookResponseDto updateBook(Long id, BookUpdateDto dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        book.setTitle(dto.getTitle().trim());
        book.setAuthor(dto.getAuthor().trim());
        book.setCategory(dto.getCategory());
        book.setPublisher(dto.getPublisher());
        book.setPublicationYear(dto.getPublicationYear());
        book.setDescription(dto.getDescription());
        book.setCoverImageUrl(dto.getCoverImageUrl());

        Book updated = bookRepository.save(book);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        boolean hasActiveCopies = book.getCopies().stream()
                .anyMatch(c -> c.getStatus() == CopyStatus.BORROWED || c.getStatus() == CopyStatus.RESERVED);

        if (hasActiveCopies) {
            throw new LibraryBusinessException("Cannot delete book. Some copies are currently borrowed or reserved.");
        }

        bookRepository.delete(book);
    }

    @Transactional
    public BookResponseDto addCopiesToBook(Long bookId, int count, String conditionNotes) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        int currentTotal = book.getCopies().size();
        for (int i = 1; i <= count; i++) {
            String barcode = generateBarcode(book.getIsbn(), currentTotal + i);
            BookCopy copy = BookCopy.builder()
                    .book(book)
                    .barcode(barcode)
                    .status(CopyStatus.AVAILABLE)
                    .acquiredDate(LocalDate.now())
                    .conditionNotes(conditionNotes != null ? conditionNotes : "New acquisition")
                    .build();
            book.addCopy(copy);
        }

        Book saved = bookRepository.save(book);
        return toResponseDto(saved);
    }

    @Transactional
    public BookCopyDto updateCopyStatus(Long copyId, BookCopyStatusUpdateDto dto) {
        BookCopy copy = bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with ID: " + copyId));

        copy.setStatus(dto.getStatus());
        if (dto.getConditionNotes() != null) {
            copy.setConditionNotes(dto.getConditionNotes());
        }

        BookCopy saved = bookCopyRepository.save(copy);
        Book book = saved.getBook();
        book.recalculateAvailableCopies();
        bookRepository.save(book);

        return toBookCopyDto(saved);
    }

    @Transactional(readOnly = true)
    public BookCopyDto getBookCopyByBarcode(String barcode) {
        BookCopy copy = bookCopyRepository.findByBarcode(barcode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with barcode: " + barcode));
        return toBookCopyDto(copy);
    }

    private String generateBarcode(String isbn, int copyIndex) {
        String cleanIsbn = isbn.replaceAll("[^a-zA-Z0-9]", "");
        String candidate = String.format("BC-%s-%02d", cleanIsbn, copyIndex);
        int attempts = copyIndex;
        while (bookCopyRepository.existsByBarcode(candidate)) {
            attempts++;
            candidate = String.format("BC-%s-%02d", cleanIsbn, attempts);
        }
        return candidate;
    }

    public BookResponseDto toResponseDto(Book book) {
        List<BookCopyDto> copyDtos = (book.getCopies() != null) ?
                book.getCopies().stream().map(this::toBookCopyDto).collect(Collectors.toList()) :
                new ArrayList<>();

        return BookResponseDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .category(book.getCategory())
                .publisher(book.getPublisher())
                .publicationYear(book.getPublicationYear())
                .totalCopies(book.getTotalCopies() != null ? book.getTotalCopies() : 0)
                .availableCopies(book.getAvailableCopies() != null ? book.getAvailableCopies() : 0)
                .description(book.getDescription())
                .coverImageUrl(book.getCoverImageUrl())
                .copies(copyDtos)
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }

    public BookCopyDto toBookCopyDto(BookCopy copy) {
        return BookCopyDto.builder()
                .id(copy.getId())
                .bookId(copy.getBook() != null ? copy.getBook().getId() : null)
                .bookTitle(copy.getBook() != null ? copy.getBook().getTitle() : null)
                .barcode(copy.getBarcode())
                .status(copy.getStatus())
                .conditionNotes(copy.getConditionNotes())
                .acquiredDate(copy.getAcquiredDate())
                .build();
    }
}
