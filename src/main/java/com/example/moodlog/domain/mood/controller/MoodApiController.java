package com.example.moodlog.domain.mood.controller;

import com.example.moodlog.domain.mood.dto.MoodRequest;
import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.service.MoodService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mood")
public class MoodApiController {

    private final MoodService moodService;
    private final UserRepository userRepository;

    // 인증에서 User 꺼내는 공통 메서드
    private User getUser(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    // 1. 오늘의 기분 기록
    @PostMapping("/write")
    public MoodRecord writeMood(@RequestBody MoodRequest request,
                                Authentication authentication) {
        User user = getUser(authentication);
        return moodService.createMood(
                user,
                request.getMood(),
                request.getMemo(),
                request.getTagText()
        );
    }

    // 2. 단일 기록 조회
    @GetMapping("/view/{id}")
    public MoodRecord viewMood(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        return moodService.getMood(id, user);
    }

    // 3. 기록 리스트 조회
    @GetMapping("/list")
    public List<MoodRecord> listMood(Authentication authentication) {
        User user = getUser(authentication);
        return moodService.findByUser(user);
    }

    // 4. 기록 수정
    @PutMapping("/edit/{id}")
    public MoodRecord editMood(@PathVariable Long id,
                               @RequestBody MoodRequest request,
                               Authentication authentication) {
        User user = getUser(authentication);
        return moodService.updateMood(
                id,
                user,
                request.getMood(),
                request.getMemo(),
                request.getTagText()
        );
    }

    // 5. 오늘 기록 여부 체크
    @GetMapping("/today")
    public Map<String, Boolean> todayMoodCheck(Authentication authentication) {
        User user = getUser(authentication);
        LocalDate today = LocalDate.now();
        boolean hasTodayRecord = moodService.existsByUserAndDate(user, today);
        Map<String, Boolean> result = new HashMap<>();
        result.put("hasTodayRecord", hasTodayRecord);
        return result;
    }
}