package com.example.marker.security;

import com.example.marker.domain.User;
import com.example.marker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        // JWT 토큰에서 추출한 ID(String)를 Long으로 변환하여 사용자 조회
        Long userId = Long.parseLong(id);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        // Spring Security의 User 객체로 변환하여 반환
        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()), // UserDetails의 username 필드에 사용자 ID를 저장
                user.getPassword(),
                new ArrayList<>() // 권한은 현재 사용하지 않으므로 빈 리스트
        );
    }
}