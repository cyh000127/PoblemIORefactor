package com.problemio.item.controller;

import com.problemio.global.auth.CustomUserDetails;
import com.problemio.item.domain.ItemType;
import com.problemio.item.dto.CustomItemResponse;
import com.problemio.item.service.CustomItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final CustomItemService customItemService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam ItemType type
    ) {
        try {
            return ResponseEntity.ok(customItemService.getAvailableItems(userDetails.getUser().getId(), type));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching my items: " + e.getMessage());
        }
    }

    @GetMapping("/definitions")
    public ResponseEntity<?> getItemDefinitions() {
        try {
            return ResponseEntity.ok(customItemService.getAllItems());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching definitions: " + e.getMessage());
        }
    }
}
