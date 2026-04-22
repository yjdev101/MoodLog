package com.example.moodlog.domain.analysis.service;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.repository.MoodRecordRepository;
import com.example.moodlog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        List<MoodRecord> records = moodRecordRepository.findByUser(user);

        if (records.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("tagStats", new HashMap<>());
            empty.put("aiComment", "아직 기록이 없어요. 기분을 기록해보세요!");
            return empty;
        }

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

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자의 기분 일기 데이터입니다.\n\n");
        prompt.append("기분 단계: VERY_GOOD(매우좋음), GOOD(좋음), NORMAL(보통), BAD(나쁨), VERY_BAD(매우나쁨)\n\n");
        prompt.append("태그별 기분 통계:\n");
        for (Map.Entry<String, Map<String, Integer>> entry : tagStats.entrySet()) {
            prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        prompt.append("\n위 데이터를 분석해서 다음을 알려주세요:\n");
        prompt.append("1. 기분이 좋을 때 자주 나타나는 태그\n");
        prompt.append("2. 기분이 나쁠 때 자주 나타나는 태그\n");
        prompt.append("3. 전반적인 감정 패턴 분석\n");
        prompt.append("친근하고 따뜻한 말투로 3~5문장으로 요약해주세요. 마크다운 문법 없이 일반 텍스트로만 작성해주세요.");

        String aiComment = claudeApiService.analyze(prompt.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("tagStats", tagStats);
        result.put("aiComment", aiComment);
        return result;
    }

    public Map<String, Object> coaching(User user, int period) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(period - 1);

        List<MoodRecord> records = moodRecordRepository.findByUserAndRecordDateBetween(user, start, end);

        if (records.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("coaching", "해당 기간에 기록이 없어요. 기분을 기록해보세요!");
            return empty;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("다음은 사용자의 최근 ").append(period).append("일간 기분 기록입니다.\n\n");
        prompt.append("기분 단계: VERY_GOOD(매우좋음), GOOD(좋음), NORMAL(보통), BAD(나쁨), VERY_BAD(매우나쁨)\n\n");
        prompt.append("날짜별 기록:\n");
        records.stream()
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(r -> {
                    prompt.append("- ").append(r.getRecordDate()).append(" / 기분: ").append(r.getMood().name());
                    if (r.getTagText() != null && !r.getTagText().isBlank())
                        prompt.append(" / 태그: ").append(r.getTagText());
                    if (r.getMemo() != null && !r.getMemo().isBlank())
                        prompt.append(" / 메모: ").append(r.getMemo());
                    prompt.append("\n");
                });
        prompt.append("\n위 데이터를 바탕으로 다음을 작성해주세요:\n");
        prompt.append("1. 이 기간의 전반적인 감정 흐름 요약 (2~3문장)\n");
        prompt.append("2. 감정에 영향을 준 주요 요인 분석 (태그/메모 기반)\n");
        prompt.append("3. 앞으로를 위한 따뜻한 조언 (2~3문장)\n");
        prompt.append("친근하고 따뜻한 말투로 작성해주세요. 마크다운 문법 없이 일반 텍스트로만 작성해주세요.");

        String coachingText = claudeApiService.analyze(prompt.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("coaching", coachingText);
        result.put("period", period);
        result.put("recordCount", records.size());
        return result;
    }
}
