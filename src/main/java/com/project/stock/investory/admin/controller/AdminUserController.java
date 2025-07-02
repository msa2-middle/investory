package com.project.stock.investory.admin.controller;

import com.project.stock.investory.admin.dto.AdminUserResponseDto;
import com.project.stock.investory.admin.dto.AdminUserUpdateRequestDto;
import com.project.stock.investory.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserResponseDto> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserResponseDto getUserById(@PathVariable Long userId) {
        return adminUserService.getUserById(userId);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminUserResponseDto updateUser(
            @PathVariable Long userId,
            @RequestBody AdminUserUpdateRequestDto requestDto
    ) {
        return adminUserService.updateUser(userId, requestDto);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
    }


}

