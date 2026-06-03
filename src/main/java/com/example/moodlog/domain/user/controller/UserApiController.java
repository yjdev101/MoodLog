package com.example.moodlog.domain.user.controller;

import com.example.moodlog.domain.auth.service.BlacklistedTokenService;
import com.example.moodlog.domain.auth.service.RefreshTokenService;
import com.example.moodlog.domain.user.dto.SignupRequest;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.service.UserService;
import com.example.moodlog.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserApiController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;
    private final BlacklistedTokenService blacklistedTokenService;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${app.base-url:http://localhost:9090}")
    private String baseUrl;

    private User getUser(Authentication authentication) {
        Long id = Long.parseLong(authentication.getName());
        return userService.findById(id);
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(
                request.getEmail(),
                request.getPassword(),
                request.getNickname()
        );
        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, Authentication authentication) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.replace("Bearer ", "");
            try {
                String jti = jwtTokenProvider.getJti(token);
                blacklistedTokenService.blacklist(jti, jwtTokenProvider.getExpiration(token));
            } catch (Exception ignored) {}
        }

        String redirectUrl = "/login.html";
        if (authentication != null) {
            User user = getUser(authentication);
            refreshTokenService.deleteByUser(user);
            if ("kakao".equals(user.getProvider()) && !kakaoClientId.isBlank()) {
                redirectUrl = "https://kauth.kakao.com/oauth/logout?client_id=" + kakaoClientId
                        + "&logout_redirect_uri=" + baseUrl + "/";
            }
        }

        SecurityContextHolder.clearContext();
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
    }

    @GetMapping("/mypage")
    public ResponseEntity<?> mypage(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증 안됨");
        }
        User user = getUser(authentication);
        return ResponseEntity.ok(user.getProfile().getNickname());
    }
}
