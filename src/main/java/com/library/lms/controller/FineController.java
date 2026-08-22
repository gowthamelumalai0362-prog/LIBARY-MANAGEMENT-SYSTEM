package com.library.lms.controller;

import com.library.lms.dto.FineDto;
import com.library.lms.dto.FinePaymentDto;
import com.library.lms.dto.FineWaiveDto;
import com.library.lms.entity.User;
import com.library.lms.service.AuthService;
import com.library.lms.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fines")
@RequiredArgsConstructor
@Tag(name = "Fines & Payments", description = "Overdue fine calculation, settlement, and fee waivers")
public class FineController {

    private final FineService fineService;
    private final AuthService authService;

    @GetMapping("/my-fines")
    @Operation(summary = "Get outstanding and historical fines for authenticated user")
    public ResponseEntity<Page<FineDto>> getMyFines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        Page<FineDto> fines = fineService.getUserFines(currentUser.getId(), PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(fines);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Get all fines across the library (Librarian/Admin)")
    public ResponseEntity<Page<FineDto>> getAllFines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(fineService.getAllFines(PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping("/{fineId}/pay")
    @Operation(summary = "Pay / settle an outstanding fine")
    public ResponseEntity<FineDto> payFine(
            @PathVariable Long fineId,
            @RequestBody(required = false) FinePaymentDto paymentDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        FineDto fine = fineService.payFine(fineId, paymentDto, userDetails.getUsername());
        return ResponseEntity.ok(fine);
    }

    @PostMapping("/{fineId}/waive")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Waive an outstanding fine with rationale (Librarian/Admin)")
    public ResponseEntity<FineDto> waiveFine(
            @PathVariable Long fineId,
            @Valid @RequestBody FineWaiveDto waiveDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        FineDto fine = fineService.waiveFine(fineId, waiveDto, userDetails.getUsername());
        return ResponseEntity.ok(fine);
    }
}
