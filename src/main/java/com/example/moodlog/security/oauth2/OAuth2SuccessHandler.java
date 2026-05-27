package com.example.moodlog.security.oauth2;

import com.example.moodlog.domain.auth.entity.RefreshToken;
import com.example.moodlog.domain.auth.service.RefreshTokenService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.entity.UserProfile;
import com.example.moodlog.domain.user.repository.UserProfileRepository;
import com.example.moodlog.domain.user.repository.UserRepository;
import com.example.moodlog.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenService refreshTokenService;
    private final OAuthTokenStore oAuthTokenStore;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        String providerId;
        String nickname;
        String profileImage;

        if ("kakao".equals(provider)) {
            providerId = oAuth2User.getAttribute("id").toString();
            Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            nickname = (String) profile.get("nickname");
            profileImage = (String) profile.getOrDefault("profile_image_url", "");
        } else if ("google".equals(provider)) {
            providerId = oAuth2User.getAttribute("sub");
            nickname = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");
            profileImage = picture != null ? picture : "";
        } else if ("naver".equals(provider)) {
            Map<String, Object> naverResponse = oAuth2User.getAttribute("response");
            providerId = (String) naverResponse.get("id");
            nickname = (String) naverResponse.getOrDefault("nickname", naverResponse.get("name"));
            profileImage = (String) naverResponse.getOrDefault("profile_image", "");
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "지원하지 않는 소셜 로그인입니다.");
            return;
        }

        final String finalProviderId = providerId;
        final String finalNickname = nickname;
        final String finalProfileImage = profileImage;

        User user = userRepository.findByProviderAndProviderId(provider, finalProviderId)
                .orElseGet(() -> {
                    User newUser = userRepository.save(
                            User.builder()
                                    .nickname(finalNickname)
                                    .profileImage(finalProfileImage)
                                    .provider(provider)
                                    .providerId(finalProviderId)
                                    .build()
                    );
                    UserProfile userProfile = new UserProfile();
                    userProfile.setUser(newUser);
                    userProfile.setNickname(finalNickname);
                    userProfile.setCreatedAt(LocalDateTime.now());
                    userProfileRepository.save(userProfile);
                    return newUser;
                });

        String accessToken = jwtTokenProvider.createToken(user.getId().toString());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, 60L * 60 * 24 * 7);

        // 토큰을 URL에 직접 노출하지 않고 30초짜리 일회용 코드로 교환
        String code = oAuthTokenStore.generateCode(accessToken, refreshToken.getToken());
        response.sendRedirect("/?code=" + code);
    }
}
