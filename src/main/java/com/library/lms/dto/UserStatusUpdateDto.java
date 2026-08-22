package com.library.lms.dto;

import com.library.lms.entity.Role;
import com.library.lms.entity.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateDto {
    @NotNull(message = "Status is required")
    private UserStatus status;
    private Role role; // Optional role update by Admin
}
