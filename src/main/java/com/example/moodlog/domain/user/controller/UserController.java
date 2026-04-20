/*
package com.example.moodlog.domain.user.controller;

import com.example.moodlog.domain.mood.entity.MoodRecord;
import com.example.moodlog.domain.mood.service.MoodService;
import com.example.moodlog.domain.user.entity.User;
import com.example.moodlog.domain.user.entity.UserProfile;
import com.example.moodlog.domain.user.service.UserProfileService;
import com.example.moodlog.domain.user.service.UserService;
import com.example.moodlog.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final MoodService moodService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 회원가입 폼
    @GetMapping("/user/signup")
    public String signupForm() {
        return "user/signup";
    }

    // 회원가입 처리
    @PostMapping("/user/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String nickname,
                         HttpServletRequest request) {

        userService.signup(email, password, nickname);
        // 회원가입 후 기존 세션 초기화
        request.getSession().invalidate();

        return "redirect:/login";
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String login() {
        return "user/login";
    }

    // 프로필 조회
    @GetMapping("/profile/{nickname}")
    public String profile(@PathVariable String nickname, Model model) {

        User user = userService.findByNickname(nickname);
        UserProfile profile = userProfileService.findByUser(user);
        List<MoodRecord> moods = moodService.findByUser(user);

        model.addAttribute("profile", profile);
        model.addAttribute("moods", moods);

        return "profile/view";
    }

    // 프로필 이미지 수정 폼
    @GetMapping("/profile/edit")
    public String editForm() {
        return "profile/edit";
    }

    // 프로필 이미지 처리
    @PostMapping("/profile/edit")
    public String editProfile(
            @RequestParam String nickname,
            @RequestParam String bio,
            @RequestParam MultipartFile imageFile,
            Principal principal
    ) {

        String email = principal.getName();
        User user = userService.findByEmail(email);

        userProfileService.updateProfile(user, nickname, bio, imageFile);

        return "redirect:/profile/" + nickname;
    }

    // 로그인 API추가
    @GetMapping("/api/login")
    @ResponseBody
    public String apiLogin(
            @RequestParam String email,
            @RequestParam String password
    ) {
        // 1. 사용자 조회
        User user = userService.findByEmail(email);

        // 2. 비밀번호 체크
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException(("비밀번호 틀림"));
        }

        // 3. JWT 생성
        String token = jwtTokenProvider.createToken(user.getNickname());
        
        // 4. 토큰 반환
        return token;
    }

    // JWT로 보호된 API만들기
    @GetMapping("/api/mypage")
    @ResponseBody
    public String mypage(Authentication authentication) {
        if (authentication == null) {
            return "인증 안됨";
        }
        return "안녕하세요, " + authentication.getPrincipal() + "님!";
    }


    // JWT 토큰 인증 테스트

    @GetMapping("/api/token")
    @ResponseBody
    public String token() {
        return jwtTokenProvider.createToken("test1");
    }

    @GetMapping("/api/test")
    @ResponseBody
    public String test(Authentication authentication) {

        if (authentication == null) {
            return "인증 안됨";
        }
        return authentication.getPrincipal().toString();
    }
}
*/
