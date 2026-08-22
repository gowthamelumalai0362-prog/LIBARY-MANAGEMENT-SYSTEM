package com.library.lms.dto;

import com.library.lms.entity.Role;
import com.library.lms.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private Role role;
    private UserStatus status;
    private String phone;
    private LocalDateTime createdAt;
    private long activeLoansCount;
    private BigDecimal unpaidFinesTotal;
    private long activeReservationsCount;
}
