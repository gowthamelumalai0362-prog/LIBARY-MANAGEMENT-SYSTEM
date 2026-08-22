package com.library.lms.controller;

import com.library.lms.dto.ReservationRequestDto;
import com.library.lms.dto.ReservationResponseDto;
import com.library.lms.entity.User;
import com.library.lms.service.AuthService;
import com.library.lms.service.ReservationService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations & Waitlist", description = "FIFO queue management and reservation lifecycle")
public class ReservationController {

    private final ReservationService reservationService;
    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Place a hold / reservation on a book")
    public ResponseEntity<ReservationResponseDto> createReservation(
            @Valid @RequestBody ReservationRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReservationResponseDto response = reservationService.createReservation(request, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an active reservation")
    public ResponseEntity<ReservationResponseDto> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReservationResponseDto response = reservationService.cancelReservation(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-reservations")
    @Operation(summary = "Get authenticated user's active and historical reservations")
    public ResponseEntity<Page<ReservationResponseDto>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        Page<ReservationResponseDto> reservations = reservationService.getUserReservations(
                currentUser.getId(), PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Get all reservations across library (Librarian/Admin)")
    public ResponseEntity<Page<ReservationResponseDto>> getAllReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reservationService.getAllReservations(
                PageRequest.of(page, size, Sort.by("reservationDate").descending())));
    }

    @GetMapping("/book/{bookId}")
    @Operation(summary = "Get FIFO reservation queue for a specific book")
    public ResponseEntity<List<ReservationResponseDto>> getBookReservations(@PathVariable Long bookId) {
        return ResponseEntity.ok(reservationService.getBookReservations(bookId));
    }
}
