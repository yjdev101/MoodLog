package com.example.moodlog.domain.mood.controller;

import com.example.moodlog.domain.mood.dto.MoodRequest;
import com.example.moodlog.domain.mood.dto.TodayCheckResponse;
import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.service.MoodService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mood")
public class MoodApiController {

    private final MoodService moodService;
    private final UserRepository userRepository;

    private User getUser(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    @PostMapping("/write")
    public ResponseEntity<MoodRecord> writeMood(@RequestBody MoodRequest request,
                                                Authentication authentication) {
        User user = getUser(authentication);
        MoodRecord record = moodService.createMood(user, request.getMood(), request.getMemo(), request.getTagText());
        return ResponseEntity.ok(record);
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<MoodRecord> viewMood(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(moodService.getMood(id, user));
    }

    @GetMapping("/list")
    public ResponseEntity<List<MoodRecord>> listMood(Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(moodService.findByUser(user));
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<MoodRecord> editMood(@PathVariable Long id,
                                               @RequestBody MoodRequest request,
                                               Authentication authentication) {
        User user = getUser(authentication);
        MoodRecord record = moodService.updateMood(id, user, request.getMood(), request.getMemo(), request.getTagText());
        return ResponseEntity.ok(record);
    }

    @GetMapping("/today")
    public ResponseEntity<TodayCheckResponse> todayMoodCheck(Authentication authentication) {
        User user = getUser(authentication);
        boolean hasTodayRecord = moodService.existsByUserAndDate(user, LocalDate.now());
        return ResponseEntity.ok(new TodayCheckResponse(hasTodayRecord));
    }
}
