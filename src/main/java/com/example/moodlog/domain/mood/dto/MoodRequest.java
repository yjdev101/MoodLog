package com.example.moodlog.domain.mood.dto;

import com.example.moodlog.domain.mood.entity.MoodType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoodRequest {
    private MoodType mood;
    private String memo;
    private String tagText; // 콤마로 구분
}
