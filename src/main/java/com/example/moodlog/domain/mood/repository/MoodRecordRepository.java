package com.example.moodlog.domain.mood.repository;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MoodRecordRepository extends JpaRepository<MoodRecord, Long> {

    // 하루 1회 기록 체크용
    boolean existsByUserAndRecordDate(User user, LocalDate recordDate);

    // 특정 기간 기록 조회용
    List<MoodRecord> findByUserAndRecordDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // 프로필 조회용
    List<MoodRecord> findByUser(User user);
}
