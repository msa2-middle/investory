package com.project.stock.investory.admin.service;

import com.project.stock.investory.admin.dto.AdminUserResponseDto;
import com.project.stock.investory.admin.dto.AdminUserUpdateRequestDto;
import com.project.stock.investory.stockAlertSetting.processor.StockPriceProcessor;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.entity.enums.Role;
import com.project.stock.investory.user.exception.InvalidRoleException;
import com.project.stock.investory.user.exception.UserNotFoundException;
import com.project.stock.investory.user.exception.UserWithdrawnException;
import com.project.stock.investory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final StockPriceProcessor stockPriceProcessor;

    public List<AdminUserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserResponseDto::fromEntity)
                .toList();
    }

    public AdminUserResponseDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());
        return AdminUserResponseDto.fromEntity(user);
    }

    @Transactional
    public AdminUserResponseDto updateUser(Long userId, AdminUserUpdateRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 이름 변경
        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.updateInfo(dto.getName(), user.getPhone());
        }

        // 전화번호 변경
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            user.updateInfo(user.getName(), dto.getPhone());
        }

        // 권한 변경
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            try {
                user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new InvalidRoleException();
            }
        }

        return AdminUserResponseDto.fromEntity(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getDeletedAt() != null) {
            throw new UserWithdrawnException();
        }

        user.withdraw();

        // 캐시  제거
        stockPriceProcessor.removeUserCache(userId);
    }
}

