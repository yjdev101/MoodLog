package com.example.moodlog.domain.mood.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TodayCheckResponse {
    private boolean hasTodayRecord;
}
