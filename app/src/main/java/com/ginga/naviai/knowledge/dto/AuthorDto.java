package com.ginga.naviai.knowledge.dto;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthorDto {
    private String id;
    private String name;
    private String role;
    /** Boolean型にすることでLombokがgetIsActive()を生成し、is_activeにシリアライズされる */
    private Boolean isActive;
}
