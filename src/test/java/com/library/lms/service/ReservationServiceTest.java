package com.library.lms.service;

import com.library.lms.dto.ReservationRequestDto;
import com.library.lms.dto.ReservationResponseDto;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookCopyRepository bookCopyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LibraryPolicyService policyService;

    @InjectMocks
    private ReservationService reservationService;

    private User member;
    private Book book;
    private LibraryPolicy policy;

    @BeforeEach
    void setUp() {
        policy = LibraryPolicy.builder()
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
                .title("Introduction to Algorithms")
                .author("CLRS")
                .isbn("9780262033848")
                .category(BookCategory.COMPUTER_SCIENCE)
                .totalCopies(2)
                .availableCopies(0)
                .copies(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should successfully place hold in FIFO reservation queue")
    void testCreateReservationSuccess() {
        when(userRepository.findByEmail("member@library.com")).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyList())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> {
            Reservation r = i.getArgument(0);
            r.setId(401L);
            r.setReservationDate(LocalDateTime.now());
            return r;
        });
        when(reservationRepository.findByBookIdAndStatusOrderByReservationDateAsc(10L, ReservationStatus.PENDING))
                .thenReturn(List.of());

        ReservationRequestDto request = ReservationRequestDto.builder()
                .bookId(10L)
                .build();

        ReservationResponseDto response = reservationService.createReservation(request, "member@library.com");

        assertNotNull(response);
        assertEquals(401L, response.getId());
        assertEquals(ReservationStatus.PENDING, response.getStatus());
        assertEquals("Introduction to Algorithms", response.getBookTitle());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Should reject reservation if user already has an active hold on this book")
    void testCreateReservationBlocked_whenAlreadyReserved() {
        when(userRepository.findByEmail("member@library.com")).thenReturn(Optional.of(member));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByUserIdAndBookIdAndStatusIn(eq(1L), eq(10L), anyList())).thenReturn(true);

        ReservationRequestDto request = ReservationRequestDto.builder()
                .bookId(10L)
                .build();

        LibraryBusinessException ex = assertThrows(LibraryBusinessException.class, () ->
                reservationService.createReservation(request, "member@library.com"));

        assertTrue(ex.getMessage().contains("already have an active hold"));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }
}
