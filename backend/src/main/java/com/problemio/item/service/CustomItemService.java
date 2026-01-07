package com.problemio.item.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.problemio.item.domain.CustomItem;
import com.problemio.item.domain.ItemType;
import com.problemio.item.dto.CustomItemRequest;
import com.problemio.item.dto.CustomItemResponse;
import com.problemio.item.mapper.CustomItemMapper;
import com.problemio.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.problemio.global.util.TimeUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomItemService {

    private final CustomItemMapper customItemMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createItem(CustomItemRequest request) {
        try {
            String configJson = objectMapper.writeValueAsString(request.getConfig());
            
            CustomItem item = CustomItem.builder()
                    .itemType(ItemType.valueOf(request.getItemType()))
                    .name(request.getName())
                    .description(request.getDescription())
                    .config(configJson)
                    .isDefault(request.isDefault())
                    .createdAt(TimeUtils.now())
                    .build();
            
            customItemMapper.save(item);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize item config", e);
            throw new RuntimeException("Invalid item configuration");
        }
    }

    public List<CustomItem> getAllItems() {
        return customItemMapper.findAll();
    }

    /*
     * 유저에게 아이템 할당
     * 유저나 아이템이 존재하지 않으면 예외 발생
     */
    @Transactional
    public void assignItemToUser(Long itemId, Long userId) {
        // 검증: 아이템 존재 여부 확인
        customItemMapper.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Invalid item ID"));
        
        // 검증: 유저 존재 여부 확인 (성능상 FK 제약에 맡길 수도 있지만 명시적 확인이 에러 메시지에 좋음) 

        // 이미 보유 중인지 확인
        if (customItemMapper.existsUserItem(userId, itemId)) {
            throw new IllegalStateException("User already owns this item");
        }

        customItemMapper.saveUserItem(userId, itemId, TimeUtils.now());
    }

    /**
     * 특정 유저가 사용 가능한 아이템 목록 조회
     * 기본(default) 아이템 + 할당된 아이템 포함
     */
    public List<CustomItemResponse> getAvailableItems(Long userId, ItemType type) {
        List<CustomItemResponse> items = customItemMapper.findItemsByUserId(userId, type);
        
        // 설정 문자열을 객체로 역직렬화
        return items.stream().map(item -> {
            try {
                Object parsedConfig = item.getConfig();
                if (parsedConfig instanceof String) {
                    parsedConfig = objectMapper.readValue((String) parsedConfig, Object.class);
                }
                
                return CustomItemResponse.builder()
                        .id(item.getId())
                        .itemType(item.getItemType())
                        .name(item.getName())
                        .description(item.getDescription())
                        .config(parsedConfig)
                        .isDefault(item.isDefault())
                        .createdAt(item.getCreatedAt())
                        .isOwned(item.isOwned())
                        .build();
            } catch (Exception e) {
                log.error("Error parsing item config for item " + item.getId(), e);
                return null; 
            }
        })
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toList());
    }

    @Transactional
    public void updateItem(Long itemId, CustomItemRequest request) {
        CustomItem item = customItemMapper.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        
        try {
            String configJson = objectMapper.writeValueAsString(request.getConfig());
            
             CustomItem updatedItem = CustomItem.builder()
                    .id(item.getId())
                    .itemType(ItemType.valueOf(request.getItemType())) 
                    .name(request.getName())
                    .description(request.getDescription())
                    .config(configJson)
                    .isDefault(request.isDefault())
                    .createdAt(item.getCreatedAt()) 
                    .build();

            customItemMapper.update(updatedItem);           
        } catch (JsonProcessingException e) {
             throw new RuntimeException("Invalid config JSON");
        }
    }

    public List<com.problemio.user.dto.UserResponse> getAssignedUsers(Long itemId) {
        return customItemMapper.findAssignedUsers(itemId).stream()
                .map(u -> com.problemio.user.dto.UserResponse.builder()
                        .id(u.getId())
                        .nickname(u.getNickname())
                        .email(u.getEmail())
                        .profileImageUrl(u.getProfileImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Autowired
    private com.problemio.global.service.S3Service s3Service;

    @Transactional
    public void deleteItem(Long itemId) {
        // 1. 설정에서 이미지 경로를 얻기 위해 아이템 조회
        CustomItem item = customItemMapper.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        
        try {
            // 설정을 파싱하여 이미지 경로 추출
            if (item.getConfig() != null) {
                // 설정이 JSON 문자열로 저장됨
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> configMap = objectMapper.readValue(item.getConfig(), java.util.Map.class);
                
                if (configMap.containsKey("image")) {
                    String imagePath = (String) configMap.get("image");
                    // S3 경로인지 확인 (public/ 시작)
                    if (imagePath != null && (imagePath.startsWith("public/") || imagePath.startsWith("/public/"))) {
                         // S3 키 일관성을 위해 선행 슬래시 제거
                         if (imagePath.startsWith("/")) imagePath = imagePath.substring(1);
                         
                         s3Service.delete(imagePath);
                    }
                }
            }
        } catch (Exception e) {
             // S3 삭제 실패 시 로그만 남기고 트랜잭션은 진행
             log.warn("Failed to delete S3 image for item " + itemId, e);
        }

        // FK 제약조건으로 인해 할당된 유저 정보 먼저 삭제
        customItemMapper.deleteUserItemsByItemId(itemId);
        customItemMapper.deleteItem(itemId);
    }

    @Transactional
    public void revokeUserItem(Long itemId, Long userId) {
        customItemMapper.deleteUserItem(userId, itemId);
    }
}
