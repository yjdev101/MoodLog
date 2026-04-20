package com.example.moodlog.security.jwt;

import com.example.moodlog.domain.user.security.CustomerUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 요청마다 토큰 검사 → 로그인 처리
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomerUserDetailsService customerUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 테스트용: URL 파라미터 token으로 테스트 가능
        if (header == null) {
            header = request.getParameter("token");
            if (header != null && !header.startsWith("Bearer ")) {
                header = "Bearer " + header;
            }
        }

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (JwtBlackList.blacklist.contains(token)) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"로그아웃된 토큰입니다.\"}");
                return;
            }

            try {
                String userId = jwtTokenProvider.getUserId(token);

                // userId가 null이 아닐 때만 userDetails 조회
                if (userId != null) {
                    var userDetails = customerUserDetailsService.loadUserByUsername(userId);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
            filterChain.doFilter(request, response);
    }
}