package com.library.lms.service;

import com.library.lms.dto.DashboardAnalyticsDto;
import com.library.lms.dto.UserProfileDto;
import com.library.lms.dto.UserStatusUpdateDto;
import com.library.lms.entity.*;
import com.library.lms.exception.LibraryBusinessException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;

    @Transactional(readOnly = true)
    public DashboardAnalyticsDto getDashboardAnalytics() {
        long totalBooks = bookRepository.count();
        long totalCopies = bookCopyRepository.count();
        long availableCopies = bookCopyRepository.countByStatus(CopyStatus.AVAILABLE);
        long borrowedCopies = bookCopyRepository.countByStatus(CopyStatus.BORROWED);
        long reservedCopies = bookCopyRepository.countByStatus(CopyStatus.RESERVED);
        long damagedLostCopies = bookCopyRepository.countByStatus(CopyStatus.DAMAGED)
                + bookCopyRepository.countByStatus(CopyStatus.LOST);

        long totalMembers = userRepository.countByRole(Role.MEMBER);
        long activeBorrowers = borrowRecordRepository.countActiveBorrowers();
        long activeLoans = borrowRecordRepository.countByStatus(BorrowStatus.ACTIVE);
        long overdueLoans = borrowRecordRepository.countByStatus(BorrowStatus.OVERDUE);
        long pendingRes = reservationRepository.countByStatus(ReservationStatus.PENDING);
        long readyPickupRes = reservationRepository.countByStatus(ReservationStatus.READY_FOR_PICKUP);

        BigDecimal revenue = fineRepository.sumTotalCollectedRevenue();
        BigDecimal pendingFines = fineRepository.sumTotalPendingFines();
        long unpaidCount = fineRepository.countByStatus(FineStatus.UNPAID);

        List<Object[]> rawTopBooks = borrowRecordRepository.findTopBorrowedBooks(PageRequest.of(0, 5));
        List<DashboardAnalyticsDto.TopBookDto> topBooks = new ArrayList<>();
        for (Object[] row : rawTopBooks) {
            topBooks.add(DashboardAnalyticsDto.TopBookDto.builder()
                    .bookId((Long) row[0])
                    .title((String) row[1])
                    .author((String) row[2])
                    .coverImageUrl((String) row[3])
                    .borrowCount((Long) row[4])
                    .build());
        }

        Map<String, Long> categoryDistribution = new LinkedHashMap<>();
        for (BookCategory cat : BookCategory.values()) {
            long count = bookRepository.findAll().stream()
                    .filter(b -> b.getCategory() == cat)
                    .count();
            if (count > 0) {
                categoryDistribution.put(cat.name(), count);
            }
        }

        return DashboardAnalyticsDto.builder()
                .totalBooks(totalBooks)
                .totalCopies(totalCopies)
                .availableCopies(availableCopies)
                .borrowedCopies(borrowedCopies)
                .reservedCopies(reservedCopies)
                .damagedLostCopies(damagedLostCopies)
                .totalMembers(totalMembers)
                .activeBorrowersCount(activeBorrowers)
                .activeLoans(activeLoans)
                .overdueLoans(overdueLoans)
                .pendingReservations(pendingRes)
                .readyForPickupReservations(readyPickupRes)
                .totalCollectedRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalPendingFines(pendingFines != null ? pendingFines : BigDecimal.ZERO)
                .unpaidFinesCount(unpaidCount)
                .topBorrowedBooks(topBooks)
                .categoryDistribution(categoryDistribution)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(u -> {
            long activeLoans = borrowRecordRepository.countByUserIdAndStatus(u.getId(), BorrowStatus.ACTIVE);
            BigDecimal unpaidFines = fineRepository.sumUnpaidFinesByUserId(u.getId());
            long activeRes = reservationRepository.findByUserIdAndStatus(u.getId(), ReservationStatus.PENDING).size()
                    + reservationRepository.findByUserIdAndStatus(u.getId(), ReservationStatus.READY_FOR_PICKUP).size();

            return UserProfileDto.builder()
                    .id(u.getId())
                    .firstName(u.getFirstName())
                    .lastName(u.getLastName())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .role(u.getRole())
                    .status(u.getStatus())
                    .phone(u.getPhone())
                    .createdAt(u.getCreatedAt())
                    .activeLoansCount(activeLoans)
                    .unpaidFinesTotal(unpaidFines != null ? unpaidFines : BigDecimal.ZERO)
                    .activeReservationsCount(activeRes)
                    .build();
        });
    }

    @Transactional
    public UserProfileDto updateUserStatus(Long userId, UserStatusUpdateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        User saved = userRepository.save(user);

        long activeLoans = borrowRecordRepository.countByUserIdAndStatus(saved.getId(), BorrowStatus.ACTIVE);
        BigDecimal unpaidFines = fineRepository.sumUnpaidFinesByUserId(saved.getId());
        long activeRes = reservationRepository.findByUserIdAndStatus(saved.getId(), ReservationStatus.PENDING).size()
                + reservationRepository.findByUserIdAndStatus(saved.getId(), ReservationStatus.READY_FOR_PICKUP).size();

        return UserProfileDto.builder()
                .id(saved.getId())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .status(saved.getStatus())
                .phone(saved.getPhone())
                .createdAt(saved.getCreatedAt())
                .activeLoansCount(activeLoans)
                .unpaidFinesTotal(unpaidFines != null ? unpaidFines : BigDecimal.ZERO)
                .activeReservationsCount(activeRes)
                .build();
    }
}
