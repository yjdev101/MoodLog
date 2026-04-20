package com.example.moodlog.domain.user.controller;

import com.example.moodlog.domain.auth.service.RefreshTokenService;
import com.example.moodlog.domain.user.dto.SignupRequest;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserRepository;
import com.example.moodlog.domain.user.service.UserService;
import com.example.moodlog.security.jwt.JwtBlackList;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserApiController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    private User getUser(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(
                request.getEmail(),
                request.getPassword(),
                request.getNickname()
        );
        return ResponseEntity.ok("회원가입 성공");
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.replace("Bearer ", "");
            JwtBlackList.blacklist.add(token);
        }
        if (authentication != null) {
            User user = getUser(authentication);
            refreshTokenService.deleteByUser(user);
        }
        return ResponseEntity.ok().build();
    }

    // 마이페이지
    @GetMapping("/mypage")
    public ResponseEntity<?> mypage(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증 안됨");
        }
        User user = getUser(authentication);
        return ResponseEntity.ok(user.getNickname());  // id 대신 nickname 반환
    }
}