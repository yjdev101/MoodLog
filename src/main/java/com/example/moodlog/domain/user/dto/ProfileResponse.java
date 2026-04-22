package com.example.moodlog.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private String nickname;
    private String bio;
    private String profileImagePath;
}
