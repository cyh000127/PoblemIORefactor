package com.problemio.item.mapper;

import com.problemio.item.domain.CustomItem;
import com.problemio.item.dto.CustomItemResponse;
import com.problemio.item.domain.ItemType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CustomItemMapper {
    void save(CustomItem customItem);
    
    Optional<CustomItem> findById(Long id);
    
    List<CustomItem> findAll();
    
    // Find items owned by user (including defaults)
    List<CustomItemResponse> findItemsByUserId(@Param("userId") Long userId, @Param("type") ItemType type);
    
    // Check ownership
    boolean existsUserItem(@Param("userId") Long userId, @Param("itemId") Long itemId);
    
    // Assign item
    void saveUserItem(@Param("userId") Long userId, @Param("itemId") Long itemId, @Param("acquiredAt") LocalDateTime acquiredAt);

    void update(CustomItem item);

    List<com.problemio.user.domain.User> findAssignedUsers(Long itemId);

    void deleteUserItem(@Param("userId") Long userId, @Param("itemId") Long itemId);

    // 아이템 삭제
    void deleteItem(Long id);

    // 아이템에 연관된 모든 유저 할당 삭제
    void deleteUserItemsByItemId(Long itemId);
}
