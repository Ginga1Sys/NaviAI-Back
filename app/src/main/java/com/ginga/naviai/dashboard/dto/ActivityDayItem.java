package com.ginga.naviai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDayItem {
    private LocalDate date;
    private int posts;
    private int comments;
    private int likes;
}
