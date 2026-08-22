package com.library.lms.service;

import com.library.lms.dto.ReservationRequestDto;
import com.library.lms.dto.ReservationResponseDto;
import com.library.lms.entity.*;
import com.library.lms.exception.LibraryBusinessException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.BookCopyRepository;
import com.library.lms.repository.BookRepository;
import com.library.lms.repository.ReservationRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final LibraryPolicyService policyService;

    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto request, String currentEmail) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        User targetUser;
        if (request.getUserId() != null && (currentUser.getRole() == Role.LIBRARIAN || currentUser.getRole() == Role.ADMIN)) {
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getUserId()));
        } else {
            targetUser = currentUser;
        }

        if (targetUser.getStatus() == UserStatus.SUSPENDED) {
            throw new LibraryBusinessException("User account is suspended and cannot place holds");
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + request.getBookId()));

        boolean alreadyReserved = reservationRepository.existsByUserIdAndBookIdAndStatusIn(
                targetUser.getId(),
                book.getId(),
                Arrays.asList(ReservationStatus.PENDING, ReservationStatus.READY_FOR_PICKUP)
        );

        if (alreadyReserved) {
            throw new LibraryBusinessException("You already have an active hold or reservation for this book");
        }

        Reservation reservation = Reservation.builder()
                .user(targetUser)
                .book(book)
                .status(ReservationStatus.PENDING)
                .notificationSent(false)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return toDto(saved);
    }

    @Transactional
    public ReservationResponseDto cancelReservation(Long reservationId, String currentEmail) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (currentUser.getRole() == Role.MEMBER && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new LibraryBusinessException("You are not authorized to cancel this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP) {
            throw new LibraryBusinessException("Only active or pending reservations can be cancelled");
        }

        BookCopy heldCopy = reservation.getHeldCopy();
        Book book = reservation.getBook();

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // If a copy was held for this reservation, reassign or make available
        if (heldCopy != null) {
            LibraryPolicy policy = policyService.getOrCreatePolicy();
            Optional<Reservation> next = reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                    book.getId(), ReservationStatus.PENDING);

            if (next.isPresent()) {
                Reservation nextRes = next.get();
                nextRes.setStatus(ReservationStatus.READY_FOR_PICKUP);
                nextRes.setHeldCopy(heldCopy);
                nextRes.setExpiryDate(LocalDateTime.now().plusHours(policy.getHoldWindowHours()));
                reservationRepository.save(nextRes);
            } else {
                heldCopy.setStatus(CopyStatus.AVAILABLE);
                bookCopyRepository.save(heldCopy);
                book.recalculateAvailableCopies();
                bookRepository.save(book);
            }
        }

        return toDto(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getUserReservations(Long userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getBookReservations(Long bookId) {
        return reservationRepository.findByBookIdAndStatusOrderByReservationDateAsc(bookId, ReservationStatus.PENDING)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processExpiredHolds() {
        LibraryPolicy policy = policyService.getOrCreatePolicy();
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredHolds = reservationRepository.findByStatusAndExpiryDateBefore(
                ReservationStatus.READY_FOR_PICKUP, now);

        for (Reservation expired : expiredHolds) {
            expired.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(expired);

            BookCopy copy = expired.getHeldCopy();
            Book book = expired.getBook();

            if (copy != null) {
                Optional<Reservation> next = reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(
                        book.getId(), ReservationStatus.PENDING);

                if (next.isPresent()) {
                    Reservation nextRes = next.get();
                    nextRes.setStatus(ReservationStatus.READY_FOR_PICKUP);
                    nextRes.setHeldCopy(copy);
                    nextRes.setExpiryDate(now.plusHours(policy.getHoldWindowHours()));
                    reservationRepository.save(nextRes);
                } else {
                    copy.setStatus(CopyStatus.AVAILABLE);
                    bookCopyRepository.save(copy);
                    book.recalculateAvailableCopies();
                    bookRepository.save(book);
                }
            }
        }
    }

    public ReservationResponseDto toDto(Reservation r) {
        Integer queuePosition = null;
        if (r.getStatus() == ReservationStatus.PENDING) {
            List<Reservation> queue = reservationRepository.findByBookIdAndStatusOrderByReservationDateAsc(
                    r.getBook().getId(), ReservationStatus.PENDING);
            for (int i = 0; i < queue.size(); i++) {
                if (queue.get(i).getId().equals(r.getId())) {
                    queuePosition = i + 1;
                    break;
                }
            }
        }

        boolean isExpired = r.getExpiryDate() != null && LocalDateTime.now().isAfter(r.getExpiryDate());

        return ReservationResponseDto.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userFullName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .bookId(r.getBook().getId())
                .bookTitle(r.getBook().getTitle())
                .bookAuthor(r.getBook().getAuthor())
                .coverImageUrl(r.getBook().getCoverImageUrl())
                .heldCopyId(r.getHeldCopy() != null ? r.getHeldCopy().getId() : null)
                .heldCopyBarcode(r.getHeldCopy() != null ? r.getHeldCopy().getBarcode() : null)
                .reservationDate(r.getReservationDate())
                .status(r.getStatus())
                .expiryDate(r.getExpiryDate())
                .queuePosition(queuePosition)
                .isExpired(isExpired)
                .build();
    }
}
