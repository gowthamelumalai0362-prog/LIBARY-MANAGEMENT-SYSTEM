package com.library.lms.scheduler;

import com.library.lms.entity.BorrowRecord;
import com.library.lms.entity.BorrowStatus;
import com.library.lms.entity.Fine;
import com.library.lms.entity.FineStatus;
import com.library.lms.entity.LibraryPolicy;
import com.library.lms.repository.BorrowRecordRepository;
import com.library.lms.repository.FineRepository;
import com.library.lms.service.LibraryPolicyService;
import com.library.lms.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LibraryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LibraryScheduler.class);

    private final BorrowRecordRepository borrowRecordRepository;
    private final FineRepository fineRepository;
    private final ReservationService reservationService;
    private final LibraryPolicyService policyService;

    /**
     * Daily background job executed at midnight (or every 30 minutes in demo/background).
     */
    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    @Transactional
    public void runDailyCirculationTasks() {
        log.info("Running daily scheduled library maintenance jobs...");
        processOverdueLoansAndFines();
        sendDueReminders();
        processHoldExpirations();
        log.info("Completed daily scheduled library maintenance jobs.");
    }

    /**
     * Periodic job running every 15 minutes to quickly detect hold expirations and overdue updates in running instance.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void periodicHoldAndOverdueCheck() {
        processOverdueLoansAndFines();
        processHoldExpirations();
    }

    @Transactional
    public void processOverdueLoansAndFines() {
        LocalDate today = LocalDate.now();
        LibraryPolicy policy = policyService.getOrCreatePolicy();

        // 1. Find all active loans whose due date is past today
        List<BorrowRecord> overdueRecords = borrowRecordRepository.findByStatusAndDueDateBefore(BorrowStatus.ACTIVE, today);
        for (BorrowRecord record : overdueRecords) {
            record.setStatus(BorrowStatus.OVERDUE);
            borrowRecordRepository.save(record);

            long daysOverdue = ChronoUnit.DAYS.between(record.getDueDate(), today);
            BigDecimal fineAmount = policy.getDailyFineRate().multiply(BigDecimal.valueOf(daysOverdue));

            // Check if fine record already exists
            Optional<Fine> existingFine = fineRepository.findByBorrowRecordId(record.getId());
            if (existingFine.isPresent()) {
                Fine f = existingFine.get();
                if (f.getStatus() == FineStatus.UNPAID) {
                    f.setAmount(fineAmount);
                    f.setReason(String.format("Overdue by %d day(s) at $%.2f/day", daysOverdue, policy.getDailyFineRate()));
                    fineRepository.save(f);
                }
            } else if (fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                Fine f = Fine.builder()
                        .borrowRecord(record)
                        .user(record.getUser())
                        .amount(fineAmount)
                        .status(FineStatus.UNPAID)
                        .reason(String.format("Overdue by %d day(s) at $%.2f/day", daysOverdue, policy.getDailyFineRate()))
                        .build();
                fineRepository.save(f);
            }
        }
        log.info("Processed {} overdue borrow records.", overdueRecords.size());
    }

    @Transactional(readOnly = true)
    public void sendDueReminders() {
        LocalDate reminderDate = LocalDate.now().plusDays(2);
        List<BorrowRecord> upcomingDue = borrowRecordRepository.findByStatusAndDueDate(BorrowStatus.ACTIVE, reminderDate);

        for (BorrowRecord record : upcomingDue) {
            log.info("Sending Due Reminder to {}: Book '{}' (Barcode: {}) is due on {}",
                    record.getUser().getEmail(),
                    record.getBookCopy().getBook().getTitle(),
                    record.getBookCopy().getBarcode(),
                    record.getDueDate());
        }
        log.info("Sent {} due date reminder notifications.", upcomingDue.size());
    }

    @Transactional
    public void processHoldExpirations() {
        reservationService.processExpiredHolds();
    }
}
