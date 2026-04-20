package com.example.moodlog.service;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.repository.MoodRecordRepository;
import com.example.moodlog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final MoodRecordRepository moodRecordRepository;
    private final ClaudeApiService claudeApiService;

    public Map<String, Object> analyze(User user) {

        // 1. 사용자의 전체 기록 가져오기
        List<MoodRecord> records = moodRecordRepository.findByUser(user);

        if (records.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("tagStats", new HashMap<>());
            empty.put("aiComment", "아직 기록이 없어요. 기분을 기록해보세요!");
            return empty;
        }

        // 2. 태그 통계 계산
        // tagStats: { "야근": { "BAD": 3, "NORMAL" : 1 }, "커피": { "GOOD" : 2 } }
        Map<String, Map<String, Integer>> tagStats = new LinkedHashMap<>();

        for (MoodRecord record : records) {
            if (record.getTagText() == null || record.getTagText().isBlank()) continue;

            String[] tags = record.getTagText().split(",");
            String mood = record.getMood().name();

            for (String tag : tags) {
                String trimmedTag = tag.trim();
                if (trimmedTag.isEmpty()) continue;

                tagStats.putIfAbsent(trimmedTag, new LinkedHashMap<>());
                tagStats.get(trimmedTag).merge(mood, 1, Integer::sum);
            }
        }

        // 3. Claude API에 보낼 프롬프트 생성
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자의 기분 일기 데이터입니다.\n\n");
        prompt.append("기분 단계: VERY_GOOD(매우좋음), GOOD(좋음), NORMAL(보통), BAD(나쁨), VERY_BAD(매우나쁨)\n\n");
        prompt.append("태그별 기분 통계:\n");

        for (Map.Entry<String, Map<String, Integer>> entry : tagStats.entrySet()) {
            prompt.append("- ").append(entry.getKey()).append(": ");
            prompt.append(entry.getValue()).append("\n");
        }

        prompt.append("\n위 데이터를 분석해서 다음을 알려주세요:\n");
        prompt.append("1. 기분이 좋을 때 자주 나타나는 태그\n");
        prompt.append("2. 기분이 나쁠 때 자주 나타나는 태그\n");
        prompt.append("3. 전반적인 감정 패턴 분석\n");
        prompt.append("친근하고 따뜻한 말투로 3~5문장으로 요약해주세요. 마크다운 문법 없이 일반 텍스트로만 작성해주세요.");

        // 4. Claude API 호출
        String aiComment = claudeApiService.analyze(prompt.toString());

        // 5. 결과 반환
        Map<String, Object> result = new HashMap<>();
        result.put("tagStats", tagStats);
        result.put("aiComment", aiComment);

        return result;
    }
}
