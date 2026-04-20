package com.example.moodlog.domain.analysis;

import com.example.moodlog.domain.mood.dto.MoodStatResponse;
import com.example.moodlog.domain.mood.service.MoodService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserRepository;
import com.example.moodlog.domain.user.security.CustomerUserDetails;
import com.example.moodlog.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
        User user = getUser(authentication);
        Map<String, Object> result = analysisService.analyze(user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/coaching")
    public ResponseEntity<Map<String, Object>> coaching(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int period) {
        if (period != 7 && period != 30) period = 7;
        User user = getUser(authentication);
        return ResponseEntity.ok(analysisService.coaching(user, period));
    }

    @GetMapping("/stats/monthly")
    @ResponseBody
    public ResponseEntity<MoodStatResponse> getMonthlyStats(
            @AuthenticationPrincipal CustomerUserDetails userDetails) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(moodService.getMonthlyStats(user));
    }
}