package com.example.moodlog.domain.user.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user")
public class User {

    // N+1 문제 (User-Post)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String email;

    @Column(nullable = true)
    private String password;
    
    @Column
    private String profileImage;    // 카카오 프로필 사진

    @Column
    private String provider;    // "kakao"  / null(일반)

    @Column
    private String providerId;  // 카카오 고유 ID

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "user")
    private UserProfile profile;
}
