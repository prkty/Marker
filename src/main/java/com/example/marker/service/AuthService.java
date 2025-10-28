package com.example.marker.service;

import com.example.marker.domain.User;
import com.example.marker.dto.AuthLoginRequest;
import com.example.marker.dto.AuthLoginResponse;
import com.example.marker.dto.AuthSignupRequest;
import com.example.marker.exception.InvalidCredentialsException;
import com.example.marker.exception.UserAlreadyExistsException;
import com.example.marker.repository.UserRepository;
import com.example.marker.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 새로운 사용자를 등록합니다.
     * @param request 회원가입 요청 DTO
     * @return 등록된 사용자의 ID
     * @throws UserAlreadyExistsException 이미 존재하는 이메일일 경우
     */
    @Transactional
    public Long signup(AuthSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // 비밀번호 암호화
                .build();
        userRepository.save(user);
        return user.getId();
    }

    /**
     * 사용자 로그인을 처리하고 JWT 토큰을 발급합니다.
     * @param request 로그인 요청 DTO
     * @return JWT 토큰을 포함한 로그인 응답 DTO
     * @throws InvalidCredentialsException 이메일 또는 비밀번호가 일치하지 않을 경우
     */
    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtTokenProvider.createToken(user.getId());
        return new AuthLoginResponse(token);
    }

    /**
     * JWT 기반 인증에서는 서버 측 로그아웃은 일반적으로 토큰 무효화(블랙리스트)를 의미하지만,
     * 이 프로젝트에서는 Refresh 토큰이나 복잡한 토큰 관리 로직이 없으므로,
     * 클라이언트 측에서 토큰을 삭제하는 것으로 로그아웃을 처리합니다.
     * 따라서 서버 측에서는 특별한 로직 없이 성공 응답을 반환합니다.
     */
    public void logout() {
        // JWT는 Stateless하므로 서버에서 특별히 할 일은 없음.
        // 클라이언트에서 토큰을 삭제하는 것으로 처리.
    }
}