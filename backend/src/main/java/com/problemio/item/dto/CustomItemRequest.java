package com.problemio.item.dto;

import com.problemio.item.domain.ItemType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@ToString
public class CustomItemRequest {
    private String itemType;
    private String name;
    private String description;
    private Object config; // JSON object from frontend
    
    @JsonProperty("isDefault")
    private boolean isDefault;
}
