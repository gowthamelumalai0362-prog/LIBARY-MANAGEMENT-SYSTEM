package com.library.lms.service;

import com.library.lms.dto.BorrowRecordDto;
import com.library.lms.dto.ReturnRequestDto;
import com.library.lms.entity.*;
import com.library.lms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnAndFineServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private FineRepository fineRepository;
    @Mock
    private LibraryPolicyService policyService;

    @InjectMocks
    private CirculationService circulationService;

    private User member;
    private User waitingMember;
    private Book book;
    private BookCopy copy;
    private LibraryPolicy policy;

    @BeforeEach
    void setUp() {
        policy = LibraryPolicy.builder()
                .maxBorrowLimit(5)
                .loanDurationDays(14)
                .dailyFineRate(new BigDecimal("0.50"))
                .fineThreshold(new BigDecimal("10.00"))
                .holdWindowHours(48)
                .build();

        member = User.builder()
                .id(1L)
                .firstName("Sophia")
                .lastName("Rodriguez")
                .email("member@library.com")
                .role(Role.MEMBER)
                .status(UserStatus.ACTIVE)
                .build();

        waitingMember = User.builder()
                .id(2L)
                .firstName("David")
                .lastName("Kim")
                .email("david@library.com")
                .role(Role.MEMBER)
                .status(UserStatus.ACTIVE)
                .build();

        book = Book.builder()
                .id(10L)
                .title("Effective Java")
                .author("Joshua Bloch")
                .isbn("9780134685991")
                .category(BookCategory.SOFTWARE_ENGINEERING)
                .totalCopies(1)
                .availableCopies(0)
                .copies(new ArrayList<>())
                .build();

        copy = BookCopy.builder()
                .id(101L)
                .book(book)
                .barcode("BC-9780134685991-01")
                .status(CopyStatus.BORROWED)
                .build();

        book.addCopy(copy);
    }

    @Test
    @DisplayName("Should return book on time without calculating fine and set copy to available")
    void testReturnOnTime_noFineCalculated() {
        BorrowRecord activeRecord = BorrowRecord.builder()
                .id(200L)
                .user(member)
                .bookCopy(copy)
                .borrowDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(4)) // Not overdue
                .status(BorrowStatus.ACTIVE)
                .renewalCount(0)
                .build();

        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(bookCopyRepository.findByBarcode("BC-9780134685991-01")).thenReturn(Optional.of(copy));
        when(borrowRecordRepository.findByBookCopyIdAndStatus(101L, BorrowStatus.ACTIVE)).thenReturn(Optional.of(activeRecord));
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(10L, ReservationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(i -> i.getArgument(0));

        ReturnRequestDto request = ReturnRequestDto.builder()
                .barcode("BC-9780134685991-01")
                .build();

        BorrowRecordDto result = circulationService.returnBook(request);

        assertNotNull(result);
        assertEquals(BorrowStatus.RETURNED, result.getStatus());
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        verify(fineRepository, never()).save(any(Fine.class));
    }

    @Test
    @DisplayName("Should calculate fine accurately when returned 5 days overdue ($0.50 * 5 = $2.50)")
    void testReturnOverdue_calculatesAccurateFine() {
        BorrowRecord overdueRecord = BorrowRecord.builder()
                .id(201L)
                .user(member)
                .bookCopy(copy)
                .borrowDate(LocalDate.now().minusDays(19))
                .dueDate(LocalDate.now().minusDays(5)) // 5 days overdue
                .status(BorrowStatus.ACTIVE)
                .renewalCount(0)
                .build();

        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(bookCopyRepository.findByBarcode("BC-9780134685991-01")).thenReturn(Optional.of(copy));
        when(borrowRecordRepository.findByBookCopyIdAndStatus(101L, BorrowStatus.ACTIVE)).thenReturn(Optional.of(overdueRecord));
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(10L, ReservationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(i -> i.getArgument(0));

        ReturnRequestDto request = ReturnRequestDto.builder()
                .barcode("BC-9780134685991-01")
                .build();

        BorrowRecordDto result = circulationService.returnBook(request);

        assertNotNull(result);
        assertEquals(BorrowStatus.RETURNED, result.getStatus());

        ArgumentCaptor<Fine> fineCaptor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository, times(1)).save(fineCaptor.capture());

        Fine savedFine = fineCaptor.getValue();
        assertEquals(0, new BigDecimal("2.50").compareTo(savedFine.getAmount()));
        assertEquals(FineStatus.UNPAID, savedFine.getStatus());
        assertEquals(member, savedFine.getUser());
    }

    @Test
    @DisplayName("Should transition returned copy to RESERVED and set oldest reservation to READY_FOR_PICKUP with 48h hold")
    void testReturnTransitionsReservationQueueToReadyForPickup() {
        BorrowRecord activeRecord = BorrowRecord.builder()
                .id(202L)
                .user(member)
                .bookCopy(copy)
                .borrowDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(9))
                .status(BorrowStatus.ACTIVE)
                .build();

        Reservation oldestPendingReservation = Reservation.builder()
                .id(300L)
                .user(waitingMember)
                .book(book)
                .status(ReservationStatus.PENDING)
                .build();

        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(bookCopyRepository.findByBarcode("BC-9780134685991-01")).thenReturn(Optional.of(copy));
        when(borrowRecordRepository.findByBookCopyIdAndStatus(101L, BorrowStatus.ACTIVE)).thenReturn(Optional.of(activeRecord));
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservationDateAsc(10L, ReservationStatus.PENDING))
                .thenReturn(Optional.of(oldestPendingReservation));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(i -> i.getArgument(0));

        ReturnRequestDto request = ReturnRequestDto.builder()
                .barcode("BC-9780134685991-01")
                .build();

        circulationService.returnBook(request);

        assertEquals(CopyStatus.RESERVED, copy.getStatus());
        assertEquals(ReservationStatus.READY_FOR_PICKUP, oldestPendingReservation.getStatus());
        assertEquals(copy, oldestPendingReservation.getHeldCopy());
        assertNotNull(oldestPendingReservation.getExpiryDate());
        verify(reservationRepository, times(1)).save(oldestPendingReservation);
    }
}
