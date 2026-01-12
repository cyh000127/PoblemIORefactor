package com.problemio.user.usecase;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.repository.UserRepository;
import com.problemio.global.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
        // Note: updatedAt is automatically handled by @LastModifiedDate if enabled, 
        // or we can set it explicitly if the entity allows.
        // The original code passed TimeUtils.now() to MyBatis mapper.
        // Assuming JPA Audit handles it, or if User entity has helper method.
        // Checking User.java again... it has @LastModifiedDate but also a updatePassword method.
        // User.java's updatePassword only sets passwordHash.
    }
}
