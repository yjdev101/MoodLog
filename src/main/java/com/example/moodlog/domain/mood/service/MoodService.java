package com.example.moodlog.domain.mood.service;

import com.example.moodlog.domain.mood.dto.MoodStatResponse;
import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.entity.MoodType;
import com.example.moodlog.domain.mood.repository.MoodRecordRepository;
import com.example.moodlog.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MoodService {

    private final MoodRecordRepository moodRecordRepository;

    // 하루 1회 기록 저장
    public MoodRecord createMood(User user, MoodType mood, String memo, String tagText) {
        LocalDate today = LocalDate.now();

        if (moodRecordRepository.existsByUserAndRecordDate(user, today)) {
            throw new IllegalStateException("오늘 이미 기록했습니다.");
        }

        MoodRecord record = MoodRecord.builder()
                .user(user)
                .mood(mood)
                .memo(memo)
                .tagText(tagText)
                .recordDate(today)
                .build();

        return moodRecordRepository.save(record);
    }

    // 특정 기간 기록 조회
    public List<MoodRecord> getRecords(User user, LocalDate startDate, LocalDate endDate) {
        return moodRecordRepository.findByUserAndRecordDateBetween(user, startDate, endDate);
    }

    // 단일 기록 조회 + 권한 체크
    public MoodRecord getMood(Long id, User user) {
        MoodRecord record = moodRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("기록을 찾을 수 없습니다"));
        if (!record.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("권한이 없습니다.");
        }
        return record;
    }

    // 기록 수정
    public MoodRecord updateMood(Long id, User user, MoodType mood, String memo, String tagText) {
        MoodRecord record = getMood(id, user);  // 권한 체크 포함

        // 오늘 기록만 수정 가능
        if (!record.getRecordDate().equals(LocalDate.now())) {
            throw new IllegalStateException("오늘 기록만 수정할 수 있습니다.");
        }

        record.setMood(mood);
        record.setMemo(memo);
        record.setTagText(tagText);
        return moodRecordRepository.save(record);
    }

    // 기록 삭제(정책상 이전 기분 기록은 삭제할 수 없으나 기능만 구현)
    public void deleteMood(Long id, User user) {
        MoodRecord record = getMood(id, user);  // 권한 체크 포함
        moodRecordRepository.delete(record);
    }

    // 오늘 기록 체크 여부
    public boolean existsByUserAndDate(User user, LocalDate date) {
        return moodRecordRepository.existsByUserAndRecordDate(user, date);
    }

    // 오늘 기록 조회
    public MoodRecord getTodayMood(User user, LocalDate date) {
        return moodRecordRepository.findByUserAndRecordDateBetween(user, date, date)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("오늘 기록이 없습니다."));
    }
    
    // 프로필에서 기분 기록 조회
    public List<MoodRecord> findByUser(User user) {
        return moodRecordRepository.findByUser(user);
    }

    // 월 별 감정 통계
    public MoodStatResponse getMonthlyStats(User user) {
        // 이번 달 1일 ~ 말일
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().withDayOfMonth(
                LocalDate.now().lengthOfMonth()
        );

        List<MoodRecord> records = moodRecordRepository
                .findByUserAndRecordDateBetween(user, start, end);

        // 감정별 횟수 집계
        Map<String, Long> moodCount = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getMood().name(),
                        Collectors.counting()
                ));

        return new MoodStatResponse(moodCount, records.size());
    }
}