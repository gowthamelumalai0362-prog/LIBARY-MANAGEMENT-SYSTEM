package com.library.lms.service;

import com.library.lms.dto.PolicyDto;
import com.library.lms.entity.LibraryPolicy;
import com.library.lms.repository.LibraryPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LibraryPolicyService {

    private final LibraryPolicyRepository policyRepository;

    @Value("${app.library.default-max-borrow-limit:5}")
    private int defaultMaxBorrowLimit;

    @Value("${app.library.default-loan-duration-days:14}")
    private int defaultLoanDurationDays;

    @Value("${app.library.default-daily-fine-rate:0.50}")
    private BigDecimal defaultDailyFineRate;

    @Value("${app.library.default-fine-threshold:10.00}")
    private BigDecimal defaultFineThreshold;

    @Value("${app.library.default-hold-window-hours:48}")
    private int defaultHoldWindowHours;

    @Transactional
    public LibraryPolicy getOrCreatePolicy() {
        return policyRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            LibraryPolicy defaultPolicy = LibraryPolicy.builder()
                    .maxBorrowLimit(defaultMaxBorrowLimit)
                    .loanDurationDays(defaultLoanDurationDays)
                    .dailyFineRate(defaultDailyFineRate)
                    .fineThreshold(defaultFineThreshold)
                    .holdWindowHours(defaultHoldWindowHours)
                    .build();
            return policyRepository.save(defaultPolicy);
        });
    }

    public PolicyDto getPolicyDto() {
        LibraryPolicy policy = getOrCreatePolicy();
        return PolicyDto.builder()
                .maxBorrowLimit(policy.getMaxBorrowLimit())
                .loanDurationDays(policy.getLoanDurationDays())
                .dailyFineRate(policy.getDailyFineRate())
                .fineThreshold(policy.getFineThreshold())
                .holdWindowHours(policy.getHoldWindowHours())
                .build();
    }

    @Transactional
    public PolicyDto updatePolicy(PolicyDto dto) {
        LibraryPolicy policy = getOrCreatePolicy();
        policy.setMaxBorrowLimit(dto.getMaxBorrowLimit());
        policy.setLoanDurationDays(dto.getLoanDurationDays());
        policy.setDailyFineRate(dto.getDailyFineRate());
        policy.setFineThreshold(dto.getFineThreshold());
        policy.setHoldWindowHours(dto.getHoldWindowHours());
        policyRepository.save(policy);
        return getPolicyDto();
    }
}
