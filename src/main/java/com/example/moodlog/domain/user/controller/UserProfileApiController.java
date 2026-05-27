package com.example.moodlog.domain.user.controller;

import com.example.moodlog.domain.user.dto.ImageUploadResponse;
import com.example.moodlog.domain.user.dto.ProfileResponse;
import com.example.moodlog.domain.user.dto.ProfileUpdateRequest;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.entity.UserProfile;
import com.example.moodlog.domain.user.repository.UserRepository;
import com.example.moodlog.domain.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
public class UserProfileApiController {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    private User getUser(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        User user = getUser(authentication);
        UserProfile profile = userProfileService.findByUser(user);
        return ResponseEntity.ok(new ProfileResponse(
                profile.getNickname(),
                profile.getBio(),
                profile.getProfileImagePath()
        ));
    }

    @PutMapping
    public ResponseEntity<String> updateProfile(@RequestBody ProfileUpdateRequest request,
                                                Authentication authentication) {
        User user = getUser(authentication);
        userProfileService.updateProfile(user, request.getNickname(), request.getBio(), null);
        return ResponseEntity.ok("프로필 수정 완료");
    }

    @PostMapping("/image")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam MultipartFile imageFile,
                                                           Authentication authentication) {
        User user = getUser(authentication);
        userProfileService.updateProfile(user, null, null, imageFile);
        UserProfile profile = userProfileService.findByUser(user);
        return ResponseEntity.ok(new ImageUploadResponse(profile.getProfileImagePath()));
    }
}
