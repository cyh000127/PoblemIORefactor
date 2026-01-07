package com.problemio.item.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomItem {
    private Long id;
    private ItemType itemType;
    private String name;
    private String description;
    private String config; // Using String for JSON content in Domain, Mapper will handle it
    @JsonProperty("isDefault")
    private boolean isDefault;
    private LocalDateTime createdAt;
}
