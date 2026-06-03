package com.example.moodlog.domain.user.service;

import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.entity.UserProfile;
import com.example.moodlog.domain.user.repository.UserProfileRepository;
import com.example.moodlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    @Value("${upload.path}")
    private String uploadPath;

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public UserProfile createProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setCreatedAt(LocalDateTime.now());

        return userProfileRepository.save(profile);
    }

    public UserProfile findByUser(User user) {
        return userProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("프로필 없음"));
    }

    // 프로필 업데이트
    public void updateProfile(User user, String nickname, String bio, MultipartFile imageFile) {
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("프로필 없음"));

        if (nickname != null && !nickname.isBlank()) {
            profile.setNickname(nickname);
            userRepository.save(user);
        }

        if (bio != null) {
            profile.setBio(bio);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
            String uploadDir = uploadPath + "/";
            String savePath = uploadDir + fileName;

            try {
                new File(uploadDir).mkdirs();
                imageFile.transferTo(new File(savePath));
            } catch (Exception e) {
                throw new RuntimeException("파일 업로드 실패", e);
            }

            profile.setProfileImagePath("/upload/" + fileName);
        }

        userProfileRepository.save(profile);
    }
}

