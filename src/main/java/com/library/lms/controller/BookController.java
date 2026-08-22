package com.library.lms.controller;

import com.library.lms.dto.*;
import com.library.lms.entity.BookCategory;
import com.library.lms.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books & Catalog", description = "Book inventory, multi-copy barcodes, and public catalog search")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Search and filter books in catalog (Public)")
    public ResponseEntity<Page<BookResponseDto>> searchBooks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookCategory category,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort sorting = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ?
                Sort.by(sortParts[0]).ascending() : Sort.by(sortParts[0]).descending();

        Page<BookResponseDto> books = bookService.searchBooks(
                search, category, availableOnly, PageRequest.of(page, size, sorting));
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed book information and its copies")
    public ResponseEntity<BookResponseDto> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Add a new book with auto-generated physical copies (Librarian/Admin)")
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookCreateDto dto) {
        BookResponseDto created = bookService.createBook(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Update book metadata (Librarian/Admin)")
    public ResponseEntity<BookResponseDto> updateBook(@PathVariable Long id, @Valid @RequestBody BookUpdateDto dto) {
        return ResponseEntity.ok(bookService.updateBook(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete book from catalog (Admin only)")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Add physical copies to existing book (Librarian/Admin)")
    public ResponseEntity<BookResponseDto> addCopies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(required = false) String conditionNotes) {
        return ResponseEntity.ok(bookService.addCopiesToBook(id, count, conditionNotes));
    }

    @PutMapping("/copies/{copyId}/status")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Update copy status (e.g. DAMAGED, LOST, AVAILABLE) (Librarian/Admin)")
    public ResponseEntity<BookCopyDto> updateCopyStatus(
            @PathVariable Long copyId,
            @Valid @RequestBody BookCopyStatusUpdateDto dto) {
        return ResponseEntity.ok(bookService.updateCopyStatus(copyId, dto));
    }

    @GetMapping("/copies/barcode/{barcode}")
    @Operation(summary = "Lookup book copy by barcode")
    public ResponseEntity<BookCopyDto> getCopyByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(bookService.getBookCopyByBarcode(barcode));
    }
}
