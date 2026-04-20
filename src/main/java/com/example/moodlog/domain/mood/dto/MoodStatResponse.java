package com.example.moodlog.domain.mood.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class MoodStatResponse {
    private Map<String, Long> moodCount;    // 감정별 횟수
    private long totalCount;                // 총 기록 횟수
}
