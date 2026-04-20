package com.example.moodlog.security.oauth2;

import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.repository.UserProfileRepository;
import com.example.moodlog.domain.user.repository.UserRepository;
import com.example.moodlog.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.example.moodlog.domain.user.entity.UserProfile;
import java.time.LocalDateTime;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 카카오 사용자 정보 추출
        String providerId = oAuth2User.getAttribute("id").toString();
        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (String) profile.get("nickname");
        String profileImage = (String) profile.getOrDefault("profile_image_url", "");

        // DB에서 기존 회원 조회 or 신규 저장
        User user = userRepository.findByProviderAndProviderId("kakao", providerId)
                .orElseGet(() -> {
                    User newUser = userRepository.save(
                            User.builder()
                                    .nickname(nickname)
                                    .profileImage(profileImage)
                                    .provider("kakao")
                                    .providerId(providerId)
                                    .build()
                    );
                    // UserProfile 자동 생성
                    UserProfile userProfile = new UserProfile();
                    userProfile.setUser(newUser);
                    userProfile.setNickname(nickname);
                    userProfile.setCreatedAt(LocalDateTime.now());
                    userProfileRepository.save(userProfile);

                    return newUser;
                });

        // JWT 발급
        String token = jwtTokenProvider.createToken(user.getId().toString());

        // 프론트로 토큰 전달 (쿼리 파라미터)
        response.sendRedirect("/?token=" + token);
    }
}