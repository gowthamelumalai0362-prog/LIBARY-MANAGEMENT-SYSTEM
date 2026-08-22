package com.library.lms.service;

import com.library.lms.dto.BorrowRecordDto;
import com.library.lms.dto.BorrowRequestDto;
import com.library.lms.dto.RenewalResponseDto;
import com.library.lms.dto.ReturnRequestDto;
import com.library.lms.entity.*;
import com.library.lms.exception.LibraryBusinessException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CirculationService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final LibraryPolicyService policyService;

    @Transactional
    public BorrowRecordDto borrowBook(BorrowRequestDto request, String currentEmail) {
        LibraryPolicy policy = policyService.getOrCreatePolicy();

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        User borrower;
        if (request.getUserId() != null && (currentUser.getRole() == Role.LIBRARIAN || currentUser.getRole() == Role.ADMIN)) {
            borrower = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Borrower user not found with ID: " + request.getUserId()));
        } else {
            borrower = currentUser;
        }

        if (borrower.getStatus() == UserStatus.SUSPENDED) {
            throw new LibraryBusinessException("User account is suspended and cannot borrow books");
        }

        long activeLoans = borrowRecordRepository.countByUserIdAndStatus(borrower.getId(), BorrowStatus.ACTIVE)
                + borrowRecordRepository.countByUserIdAndStatus(borrower.getId(), BorrowStatus.OVERDUE);
        if (activeLoans >= policy.getMaxBorrowLimit()) {
            throw new LibraryBusinessException("Borrow limit reached. Maximum allowed active loans is " + policy.getMaxBorrowLimit());
        }

        BigDecimal unpaidFines = fineRepository.sumUnpaidFinesByUserId(borrower.getId());
        if (unpaidFines != null && unpaidFines.compareTo(policy.getFineThreshold()) > 0) {
            throw new LibraryBusinessException(String.format(
                    "Borrowing blocked: Outstanding fines ($%.2f) exceed the allowed threshold ($%.2f)",
                    unpaidFines, policy.getFineThreshold()));
        }

        BookCopy copy = bookCopyRepository.findByBarcode(request.getBarcode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with barcode: " + request.getBarcode()));

        Book book = copy.getBook();

        // Check if copy is reserved
        if (copy.getStatus() == CopyStatus.RESERVED) {
            Optional<Reservation> userReservation = reservationRepository.findByBookIdAndStatusOrderByReservationDateAsc(book.getId(), ReservationStatus.READY_FOR_PICKUP)
                    .stream()
                    .filter(r -> r.getUser().getId().equals(borrower.getId()) && (r.getHeldCopy() == null || r.getHeldCopy().getId().equals(copy.getId())))
                    .findFirst();

            if (userReservation.isPresent()) {
                Reservation res = userReservation.get();
                res.setStatus(ReservationStatus.COMPLETED);
                reservationRepository.save(res);
            } else {
                throw new LibraryBusinessException("This book copy is currently reserved for another patron");
            }
        } else if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new LibraryBusinessException("Book copy is not available for borrowing (Current status: " + copy.getStatus() + ")");
        }

        // If borrower had a pending reservation for this book, mark it as completed
        reservationRepository.findByBookIdAndStatusOrderByReservationDateAsc(book.getId(), ReservationStatus.PENDING)
                .stream()
                .filter(r -> r.getUser().getId().equals(borrower.getId()))
                .findFirst()
                .ifPresent(res -> {
                    res.setStatus(ReservationStatus.COMPLETED);
                    reservationRepository.save(res);
                });

        copy.setStatus(CopyStatus.BORROWED);
        bookCopyRepository.save(copy);

        book.recalculateAvailableCopies();
        bookRepository.save(book);

        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.plusDays(policy.getLoanDurationDays());

        BorrowRecord record = BorrowRecord.builder()
                .user(borrower)
                .bookCopy(copy)
                .borrowDate(today)
                .dueDate(dueDate)
                .status(BorrowStatus.ACTIVE)
                .renewalCount(0)
                .build();

        BorrowRecord savedRecord = borrowRecordRepository.save(record);
        return toDto(savedRecord);
    }

    @Transactional
    public BorrowRecordDto returnBook(ReturnRequestDto request) {
        LibraryPolicy policy = policyService.getOrCreatePolicy();

        BookCopy copy = bookCopyRepository.findByBarcode(request.getBarcode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with barcode: " + request.getBarcode()));

        BorrowRecord record = borrowRecordRepository.findByBookCopyIdAndStatus(copy.getId(), BorrowStatus.ACTIVE)
                .or(() -> borrowRecordRepository.findByBookCopyIdAndStatus(copy.getId(), BorrowStatus.OVERDUE))
                .orElseThrow(() -> new LibraryBusinessException("No active loan record found for book copy: " + request.getBarcode()));

        LocalDate returnDate = LocalDate.now();
        record.setReturnDate(returnDate);
        record.setStatus(BorrowStatus.RETURNED);

        // Fine calculation
        if (returnDate.isAfter(record.getDueDate())) {
            long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
            BigDecimal fineAmount = policy.getDailyFineRate().multiply(BigDecimal.valueOf(daysOverdue));

            if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                Fine fine = Fine.builder()
                        .borrowRecord(record)
                        .user(record.getUser())
                        .amount(fineAmount)
                        .status(FineStatus.UNPAID)
                        .reason(String.format("Overdue by %d day(s) at $%.2f/day", daysOverdue, policy.getDailyFineRate()))
                        .build();
                fineRepository.save(fine);
            }
        }

        if (request.getConditionNotes() != null && !request.getConditionNotes().trim().isEmpty()) {
            copy.setConditionNotes(request.getConditionNotes().trim());
        }

        Book book = copy.getBook();

        // FIFO Waitlist queue check
        Optional<Reservation> nextReservation = reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                book.getId(), ReservationStatus.PENDING);

        if (nextReservation.isPresent()) {
            Reservation reservation = nextReservation.get();
            reservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
            reservation.setHeldCopy(copy);
            reservation.setExpiryDate(LocalDateTime.now().plusHours(policy.getHoldWindowHours()));
            reservationRepository.save(reservation);

            copy.setStatus(CopyStatus.RESERVED);
        } else {
            copy.setStatus(CopyStatus.AVAILABLE);
        }

        bookCopyRepository.save(copy);
        book.recalculateAvailableCopies();
        bookRepository.save(book);

        BorrowRecord savedRecord = borrowRecordRepository.save(record);
        return toDto(savedRecord);
    }

    @Transactional
    public RenewalResponseDto renewLoan(Long borrowRecordId, String currentEmail) {
        LibraryPolicy policy = policyService.getOrCreatePolicy();

        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow record not found with ID: " + borrowRecordId));

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (currentUser.getRole() == Role.MEMBER && !record.getUser().getId().equals(currentUser.getId())) {
            throw new LibraryBusinessException("You are not authorized to renew this loan record");
        }

        if (record.getStatus() != BorrowStatus.ACTIVE && record.getStatus() != BorrowStatus.OVERDUE) {
            throw new LibraryBusinessException("Only active loans can be renewed");
        }

        // Check if there are waiting reservations for this book
        long waitingReservations = reservationRepository.countByBookIdAndStatus(
                record.getBookCopy().getBook().getId(), ReservationStatus.PENDING);
        if (waitingReservations > 0) {
            throw new LibraryBusinessException("Renewal rejected: Other patrons are waiting in the reservation queue for this book");
        }

        // Check unpaid fine threshold
        BigDecimal unpaidFines = fineRepository.sumUnpaidFinesByUserId(record.getUser().getId());
        if (unpaidFines != null && unpaidFines.compareTo(policy.getFineThreshold()) > 0) {
            throw new LibraryBusinessException(String.format(
                    "Renewal blocked: Outstanding fines ($%.2f) exceed threshold ($%.2f)",
                    unpaidFines, policy.getFineThreshold()));
        }

        LocalDate oldDueDate = record.getDueDate();
        LocalDate newDueDate = (oldDueDate.isBefore(LocalDate.now()) ? LocalDate.now() : oldDueDate)
                .plusDays(policy.getLoanDurationDays());

        record.setDueDate(newDueDate);
        record.setStatus(BorrowStatus.ACTIVE);
        record.setRenewalCount(record.getRenewalCount() + 1);

        borrowRecordRepository.save(record);

        return RenewalResponseDto.builder()
                .borrowRecordId(record.getId())
                .oldDueDate(oldDueDate)
                .newDueDate(newDueDate)
                .renewalCount(record.getRenewalCount())
                .message("Loan successfully renewed until " + newDueDate)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecordDto> getUserLoans(Long userId, Pageable pageable) {
        return borrowRecordRepository.findByUserId(userId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecordDto> getActiveLoans(Pageable pageable) {
        return borrowRecordRepository.findByStatus(BorrowStatus.ACTIVE, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BorrowRecordDto> getOverdueLoans(Pageable pageable) {
        return borrowRecordRepository.findByStatus(BorrowStatus.OVERDUE, pageable).map(this::toDto);
    }

    public BorrowRecordDto toDto(BorrowRecord record) {
        LibraryPolicy policy = policyService.getOrCreatePolicy();
        LocalDate today = LocalDate.now();
        boolean isOverdue = record.getStatus() == BorrowStatus.ACTIVE && today.isAfter(record.getDueDate());
        long daysOverdue = 0;
        BigDecimal currentFine = BigDecimal.ZERO;

        if (record.getStatus() == BorrowStatus.RETURNED && record.getReturnDate() != null && record.getReturnDate().isAfter(record.getDueDate())) {
            daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), record.getReturnDate());
            currentFine = policy.getDailyFineRate().multiply(BigDecimal.valueOf(daysOverdue));
        } else if (isOverdue) {
            daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);
            currentFine = policy.getDailyFineRate().multiply(BigDecimal.valueOf(daysOverdue));
        }

        BookCopy copy = record.getBookCopy();
        Book book = copy != null ? copy.getBook() : null;

        return BorrowRecordDto.builder()
                .id(record.getId())
                .userId(record.getUser().getId())
                .userFullName(record.getUser().getFullName())
                .userEmail(record.getUser().getEmail())
                .bookId(book != null ? book.getId() : null)
                .bookTitle(book != null ? book.getTitle() : "Unknown")
                .bookAuthor(book != null ? book.getAuthor() : "Unknown")
                .coverImageUrl(book != null ? book.getCoverImageUrl() : null)
                .bookCopyId(copy != null ? copy.getId() : null)
                .barcode(copy != null ? copy.getBarcode() : null)
                .borrowDate(record.getBorrowDate())
                .dueDate(record.getDueDate())
                .returnDate(record.getReturnDate())
                .status(record.getStatus())
                .renewalCount(record.getRenewalCount())
                .daysOverdue(daysOverdue)
                .currentFine(currentFine)
                .isOverdue(isOverdue)
                .build();
    }
}
