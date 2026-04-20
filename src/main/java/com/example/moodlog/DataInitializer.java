package com.example.moodlog;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.entity.MoodType;
import com.example.moodlog.domain.mood.repository.MoodRecordRepository;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.entity.UserProfile;
import com.example.moodlog.domain.user.repository.UserProfileRepository;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MoodRecordRepository moodRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {

        // email, password nullable 자동 적용
        jdbcTemplate.execute("ALTER TABLE user MODIFY COLUMN email VARCHAR(255) NULL");
        jdbcTemplate.execute("ALTER TABLE user MODIFY COLUMN password VARCHAR(255) NULL");

        // 이미 테스트 유저가 있으면 스킵
        if (userRepository.existsByEmail("test@test.com")) {
            System.out.println("더미 데이터가 이미 존재합니다. 스킵합니다.");
            return;
        }

        // 나머지 기존 코드 동일
        System.out.println("더미 데이터 초기화 시작...");

        User user = User.builder()
                .email("test@test.com")
                .password(passwordEncoder.encode("test123$"))
                .nickname("테스트유저")
                .build();
        userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setNickname("테스트유저");
        profile.setCreatedAt(LocalDateTime.now());
        userProfileRepository.save(profile);

        LocalDate today = LocalDate.now();

        Object[][] data = {
                {-14, MoodType.VERY_BAD,  "야근이 너무 힘들다", "야근, 피곤, 스트레스"},
                {-13, MoodType.BAD,       "회의가 너무 많았다", "회의, 피곤, 야근"},
                {-12, MoodType.NORMAL,    "그럭저럭 괜찮은 하루", "커피, 산책"},
                {-11, MoodType.GOOD,      "운동하고 나니 기분이 좋다", "운동, 커피"},
                {-10, MoodType.VERY_GOOD, "친구들과 즐거운 시간", "친구, 맛집, 여가"},
                {-9,  MoodType.BAD,       "또 야근...", "야근, 피곤, 스트레스"},
                {-8,  MoodType.NORMAL,    "평범한 하루였다", "커피, 독서"},
                {-7,  MoodType.GOOD,      "독서하며 힐링했다", "독서, 커피, 여가"},
                {-6,  MoodType.VERY_BAD,  "프로젝트 마감 스트레스", "야근, 스트레스, 피곤"},
                {-5,  MoodType.BAD,       "몸이 안좋다", "피곤, 두통"},
                {-4,  MoodType.NORMAL,    "조금 나아졌다", "산책, 커피"},
                {-3,  MoodType.GOOD,      "운동으로 스트레스 해소", "운동, 산책"},
                {-2,  MoodType.VERY_GOOD, "맛있는 거 먹고 행복", "맛집, 친구, 여가"},
                {-1,  MoodType.GOOD,      "여유로운 하루", "독서, 커피, 산책"},
                {0,   MoodType.NORMAL,    "오늘도 화이팅", "커피, 코딩"},
        };

        for (Object[] row : data) {
            int offset = (int) row[0];
            MoodType mood = (MoodType) row[1];
            String memo = (String) row[2];
            String tagText = (String) row[3];

            MoodRecord record = MoodRecord.builder()
                    .user(user)
                    .mood(mood)
                    .memo(memo)
                    .tagText(tagText)
                    .recordDate(today.plusDays(offset))
                    .build();

            moodRecordRepository.save(record);
        }

        System.out.println("더미 데이터 초기화 완료!");
    }
}