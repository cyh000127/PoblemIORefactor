package com.problemio.user.usecase;

import com.problemio.global.exception.BusinessException;
import com.problemio.global.exception.ErrorCode;
import com.problemio.user.domain.DeleteStatus;
import com.problemio.user.domain.User;
import com.problemio.user.dto.UserResponse;
import com.problemio.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUserUseCase {

    private final UserRepository userRepository;

    public UserResponse execute(Long userId) {
        User user = userRepository.findByIdAndIsDeleted(userId, DeleteStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        return UserResponse.from(user);
    }
}
