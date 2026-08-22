package com.library.lms.service;

import com.library.lms.dto.FineDto;
import com.library.lms.dto.FinePaymentDto;
import com.library.lms.dto.FineWaiveDto;
import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.Fine;
import com.library.lms.entity.FineStatus;
import com.library.lms.entity.Role;
import com.library.lms.entity.User;
import com.library.lms.exception.LibraryBusinessException;
import com.library.lms.exception.ResourceNotFoundException;
import com.library.lms.repository.FineRepository;
import com.library.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<FineDto> getUserFines(Long userId, Pageable pageable) {
        return fineRepository.findByUserId(userId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<FineDto> getAllFines(Pageable pageable) {
        return fineRepository.findAll(pageable).map(this::toDto);
    }

    @Transactional
    public FineDto payFine(Long fineId, FinePaymentDto paymentDto, String currentEmail) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found with ID: " + fineId));

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (currentUser.getRole() == Role.MEMBER && !fine.getUser().getId().equals(currentUser.getId())) {
            throw new LibraryBusinessException("You are not authorized to pay another member's fine");
        }

        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new LibraryBusinessException("Fine is already settled (Status: " + fine.getStatus() + ")");
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod(paymentDto != null && paymentDto.getPaymentMethod() != null ?
                paymentDto.getPaymentMethod() : "ONLINE");

        Fine saved = fineRepository.save(fine);
        return toDto(saved);
    }

    @Transactional
    public FineDto waiveFine(Long fineId, FineWaiveDto waiveDto, String currentEmail) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found with ID: " + fineId));

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        if (currentUser.getRole() != Role.LIBRARIAN && currentUser.getRole() != Role.ADMIN) {
            throw new LibraryBusinessException("Only Librarians and Admins can waive fines");
        }

        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new LibraryBusinessException("Fine is not in UNPAID status");
        }

        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedReason(waiveDto != null ? waiveDto.getReason() : "Waived by staff");
        fine.setPaidAt(LocalDateTime.now());

        Fine saved = fineRepository.save(fine);
        return toDto(saved);
    }

    public FineDto toDto(Fine f) {
        BorrowRecord record = f.getBorrowRecord();
        String bookTitle = (record != null && record.getBookCopy() != null && record.getBookCopy().getBook() != null) ?
                record.getBookCopy().getBook().getTitle() : "N/A";
        String barcode = (record != null && record.getBookCopy() != null) ?
                record.getBookCopy().getBarcode() : "N/A";

        return FineDto.builder()
                .id(f.getId())
                .borrowRecordId(record != null ? record.getId() : null)
                .userId(f.getUser().getId())
                .userFullName(f.getUser().getFullName())
                .userEmail(f.getUser().getEmail())
                .bookTitle(bookTitle)
                .barcode(barcode)
                .amount(f.getAmount())
                .status(f.getStatus())
                .reason(f.getReason())
                .paidAt(f.getPaidAt())
                .paymentMethod(f.getPaymentMethod())
                .waivedReason(f.getWaivedReason())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
