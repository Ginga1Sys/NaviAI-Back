package com.ginga.naviai.knowledge.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TagDto {
    private String id;
    private String name;
}
