package com.example.moodlog.domain.analysis;

import com.example.moodlog.domain.analysis.service.AnalysisService;
import com.example.moodlog.domain.mood.dto.MoodStatResponse;
import com.example.moodlog.domain.mood.service.MoodService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final MoodService moodService;
    private final UserRepository userRepository;

    private User getUser(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> analyze(Authentication authentication) {
        return ResponseEntity.ok(analysisService.analyze(getUser(authentication)));
    }

    @GetMapping("/coaching")
    public ResponseEntity<Map<String, Object>> coaching(Authentication authentication,
                                                        @RequestParam(defaultValue = "7") int period) {
        if (period != 7 && period != 30) period = 7;
        return ResponseEntity.ok(analysisService.coaching(getUser(authentication), period));
    }

    @GetMapping("/stats/monthly")
    public ResponseEntity<MoodStatResponse> getMonthlyStats(Authentication authentication) {
        return ResponseEntity.ok(moodService.getMonthlyStats(getUser(authentication)));
    }
}
