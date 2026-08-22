package com.library.lms.controller;

import com.library.lms.dto.BorrowRecordDto;
import com.library.lms.dto.BorrowRequestDto;
import com.library.lms.dto.RenewalResponseDto;
import com.library.lms.dto.ReturnRequestDto;
import com.library.lms.service.CirculationService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/circulation")
@RequiredArgsConstructor
@Tag(name = "Circulation", description = "Borrowing, return processing, renewals, and loan lifecycle")
public class CirculationController {

    private final CirculationService circulationService;
    private final com.library.lms.service.AuthService authService;

    @PostMapping("/borrow")
    @Operation(summary = "Issue / borrow a book copy by barcode")
    public ResponseEntity<BorrowRecordDto> borrowBook(
            @Valid @RequestBody BorrowRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BorrowRecordDto record = circulationService.borrowBook(request, userDetails.getUsername());
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }

    @PostMapping("/return")
    @Operation(summary = "Return a borrowed book copy & calculate fines if overdue")
    public ResponseEntity<BorrowRecordDto> returnBook(@Valid @RequestBody ReturnRequestDto request) {
        BorrowRecordDto record = circulationService.returnBook(request);
        return ResponseEntity.ok(record);
    }

    @PostMapping("/renew/{borrowRecordId}")
    @Operation(summary = "Extend loan due date if no pending reservations exist")
    public ResponseEntity<RenewalResponseDto> renewLoan(
            @PathVariable Long borrowRecordId,
            @AuthenticationPrincipal UserDetails userDetails) {
        RenewalResponseDto response = circulationService.renewLoan(borrowRecordId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-loans")
    @Operation(summary = "Get current authenticated user's loan history")
    public ResponseEntity<Page<BorrowRecordDto>> getMyLoans(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.library.lms.entity.User currentUser = authService.getCurrentAuthenticatedUser();
        Page<BorrowRecordDto> loans = circulationService.getUserLoans(currentUser.getId(), PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Get loan records for a specific user (Librarian/Admin)")
    public ResponseEntity<Page<BorrowRecordDto>> getUserLoans(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(circulationService.getUserLoans(userId, PageRequest.of(page, size, Sort.by("id").descending())));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Get all currently active loans across library (Librarian/Admin)")
    public ResponseEntity<Page<BorrowRecordDto>> getActiveLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(circulationService.getActiveLoans(PageRequest.of(page, size, Sort.by("borrowDate").descending())));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Get all overdue loans (Librarian/Admin)")
    public ResponseEntity<Page<BorrowRecordDto>> getOverdueLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(circulationService.getOverdueLoans(PageRequest.of(page, size, Sort.by("dueDate").ascending())));
    }
}
