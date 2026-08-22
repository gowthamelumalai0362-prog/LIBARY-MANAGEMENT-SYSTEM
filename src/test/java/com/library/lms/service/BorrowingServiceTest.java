package com.library.lms.service;

import com.library.lms.dto.BorrowRecordDto;
import com.library.lms.dto.BorrowRequestDto;
import com.library.lms.entity.*;
import com.library.lms.exception.LibraryBusinessException;
import com.library.lms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class BorrowingServiceTest {

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

        book = Book.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .category(BookCategory.SOFTWARE_ENGINEERING)
                .totalCopies(2)
                .availableCopies(2)
                .copies(new ArrayList<>())
                .build();

        copy = BookCopy.builder()
                .id(101L)
                .book(book)
                .barcode("BC-9780132350884-01")
                .status(CopyStatus.AVAILABLE)
                .build();

        book.addCopy(copy);
    }

    @Test
    @DisplayName("Should successfully borrow book copy when copies available, limit not reached, and no unpaid fines")
    void testBorrowSuccess_whenCopiesAvailableAndNoFines() {
        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(userRepository.findByEmail("member@library.com")).thenReturn(Optional.of(member));
        when(borrowRecordRepository.countByUserIdAndStatus(1L, BorrowStatus.ACTIVE)).thenReturn(2L);
        when(borrowRecordRepository.countByUserIdAndStatus(1L, BorrowStatus.OVERDUE)).thenReturn(0L);
        when(fineRepository.sumUnpaidFinesByUserId(1L)).thenReturn(BigDecimal.ZERO);
        when(bookCopyRepository.findByBarcode("BC-9780132350884-01")).thenReturn(Optional.of(copy));
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord r = invocation.getArgument(0);
            r.setId(500L);
            return r;
        });

        BorrowRequestDto request = BorrowRequestDto.builder()
                .barcode("BC-9780132350884-01")
                .build();

        BorrowRecordDto result = circulationService.borrowBook(request, "member@library.com");

        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals("Clean Code", result.getBookTitle());
        assertEquals(CopyStatus.BORROWED, copy.getStatus());
        assertEquals(LocalDate.now().plusDays(14), result.getDueDate());
        verify(borrowRecordRepository, times(1)).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Should reject borrow when member exceeds max borrowing limit")
    void testBorrowBlocked_whenExceedsMaxBorrowLimit() {
        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(userRepository.findByEmail("member@library.com")).thenReturn(Optional.of(member));
        when(borrowRecordRepository.countByUserIdAndStatus(1L, BorrowStatus.ACTIVE)).thenReturn(5L);
        when(borrowRecordRepository.countByUserIdAndStatus(1L, BorrowStatus.OVERDUE)).thenReturn(0L);

        BorrowRequestDto request = BorrowRequestDto.builder()
                .barcode("BC-9780132350884-01")
                .build();

        LibraryBusinessException ex = assertThrows(LibraryBusinessException.class, () ->
                circulationService.borrowBook(request, "member@library.com"));

        assertTrue(ex.getMessage().contains("Borrow limit reached"));
        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Should reject borrow when member has unpaid fines exceeding threshold")
    void testBorrowBlocked_whenUnpaidFinesExceedThreshold() {
        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(userRepository.findByEmail("member@library.com")).thenReturn(Optional.of(member));
        when(borrowRecordRepository.countByUserIdAndStatus(1L, BorrowStatus.ACTIVE)).thenReturn(1L);
        when(borrowRecordRepository.countByUserIdAndStatus(1L, BorrowStatus.OVERDUE)).thenReturn(0L);
        when(fineRepository.sumUnpaidFinesByUserId(1L)).thenReturn(new BigDecimal("15.50"));

        BorrowRequestDto request = BorrowRequestDto.builder()
                .barcode("BC-9780132350884-01")
                .build();

        LibraryBusinessException ex = assertThrows(LibraryBusinessException.class, () ->
                circulationService.borrowBook(request, "member@library.com"));

        assertTrue(ex.getMessage().contains("exceed the allowed threshold"));
        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Should reject borrow when user account is suspended")
    void testBorrowBlocked_whenUserSuspended() {
        member.setStatus(UserStatus.SUSPENDED);
        when(policyService.getOrCreatePolicy()).thenReturn(policy);
        when(userRepository.findByEmail("member@library.com")).thenReturn(Optional.of(member));

        BorrowRequestDto request = BorrowRequestDto.builder()
                .barcode("BC-9780132350884-01")
                .build();

        LibraryBusinessException ex = assertThrows(LibraryBusinessException.class, () ->
                circulationService.borrowBook(request, "member@library.com"));

        assertTrue(ex.getMessage().contains("User account is suspended"));
    }
}
