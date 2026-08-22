package com.library.lms.controller;

import com.library.lms.dto.DashboardAnalyticsDto;
import com.library.lms.dto.PolicyDto;
import com.library.lms.dto.UserProfileDto;
import com.library.lms.dto.UserStatusUpdateDto;
import com.library.lms.scheduler.LibraryScheduler;
import com.library.lms.service.AdminService;
import com.library.lms.service.LibraryPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin & Analytics", description = "System metrics, user privileges, library policies, and automated jobs")
public class AdminController {

    private final AdminService adminService;
    private final LibraryPolicyService policyService;
    private final LibraryScheduler libraryScheduler;

    @GetMapping("/analytics/dashboard")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Get overall library circulation and financial analytics (Librarian/Admin)")
    public ResponseEntity<DashboardAnalyticsDto> getDashboardAnalytics() {
        return ResponseEntity.ok(adminService.getDashboardAnalytics());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all registered users with privileges and loan statuses (Admin only)")
    public ResponseEntity<Page<UserProfileDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(PageRequest.of(page, size, Sort.by("id").ascending())));
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user status (ACTIVE/SUSPENDED) and role (Admin only)")
    public ResponseEntity<UserProfileDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDto dto) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, dto));
    }

    @GetMapping("/policies")
    @Operation(summary = "Get current library policies (Public/All)")
    public ResponseEntity<PolicyDto> getPolicies() {
        return ResponseEntity.ok(policyService.getPolicyDto());
    }

    @PutMapping("/policies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update library policies: loan limit, duration, daily fine rate, hold window (Admin only)")
    public ResponseEntity<PolicyDto> updatePolicies(@Valid @RequestBody PolicyDto dto) {
        return ResponseEntity.ok(policyService.updatePolicy(dto));
    }

    @PostMapping("/scheduler/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger the daily scheduled jobs: fine calculation, hold expirations (Admin only)")
    public ResponseEntity<Map<String, String>> triggerSchedulerJobs() {
        libraryScheduler.processOverdueLoansAndFines();
        libraryScheduler.sendDueReminders();
        libraryScheduler.processHoldExpirations();
        return ResponseEntity.ok(Map.of("message", "Scheduled background jobs executed successfully."));
    }
}
