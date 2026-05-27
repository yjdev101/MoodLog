package com.example.moodlog.domain.mood.service;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.entity.MoodType;
import com.example.moodlog.domain.mood.repository.MoodRecordRepository;
import com.example.moodlog.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MoodServiceTest {

    @Mock
    private MoodRecordRepository moodRecordRepository;

    @InjectMocks
    private MoodService moodService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .nickname("테스터")
                .build();
    }

    @Test
    void 오늘_이미_기록했으면_예외가_발생한다() {
        // given
        LocalDate today = LocalDate.now();
        given(moodRecordRepository.existsByUserAndRecordDate(user, today))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                moodService.createMood(user, MoodType.GOOD, "메모", "태그"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("오늘 이미 기록했습니다.");
    }

    @Test
    void 오늘_기록이_없으면_정상적으로_저장된다() {
        // given
        LocalDate today = LocalDate.now();
        given(moodRecordRepository.existsByUserAndRecordDate(user, today))
                .willReturn(false);

        MoodRecord record = MoodRecord.builder()
                .user(user)
                .mood(MoodType.GOOD)
                .memo("메모")
                .tagText("태그")
                .recordDate(today)
                .build();

        given(moodRecordRepository.save(any())).willReturn(record);

        // when
        MoodRecord result = moodService.createMood(user, MoodType.GOOD, "메모", "태그");

        // then
        assertThat(result.getMood()).isEqualTo(MoodType.GOOD);
        assertThat(result.getMemo()).isEqualTo("메모");
    }

    @Test
    void 존재하는_기록을_조회하면_정상_반환된다() {
        // given
        MoodRecord record = MoodRecord.builder()
                .user(user)
                .mood(MoodType.GOOD)
                .memo("메모")
                .tagText("태그")
                .recordDate(LocalDate.now())
                .build();

        given(moodRecordRepository.findById(1L))
                .willReturn(Optional.of(record));

        // when
        MoodRecord result = moodService.getMood(1L, user);

        // then
        assertThat(result.getMood()).isEqualTo(MoodType.GOOD);
        assertThat(result.getMemo()).isEqualTo("메모");
    }

    @Test
    void 다른_유저의_기록을_조회하면_예외가_발생한다() {
        // given
        User otherUser = User.builder()
                .id(2L)
                .email("other@test.com")
                .nickname("다른유저")
                .build();

        MoodRecord record = MoodRecord.builder()
                .user(otherUser)
                .mood(MoodType.GOOD)
                .memo("메모")
                .tagText("태그")
                .recordDate(LocalDate.now())
                .build();

        given(moodRecordRepository.findById(1L))
                .willReturn(Optional.of(record));

        // when & then
        assertThatThrownBy(() -> moodService.getMood(1L, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("권한이 없습니다.");
    }

    @Test
    void 오늘_기록을_수정하면_정상_저장된다() {
        // given
        MoodRecord record = MoodRecord.builder()
                .user(user)
                .mood(MoodType.GOOD)
                .memo("메모")
                .tagText("태그")
                .recordDate(LocalDate.now())
                .build();

        given(moodRecordRepository.findById(1L))
                .willReturn(Optional.of(record));

        // when

        // then
    }
}
