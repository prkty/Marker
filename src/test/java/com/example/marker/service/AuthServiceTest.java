package com.example.marker.service;

import com.example.marker.domain.User;
import com.example.marker.dto.AuthLoginRequest;
import com.example.marker.dto.AuthLoginResponse;
import com.example.marker.dto.AuthSignupRequest;
import com.example.marker.exception.InvalidCredentialsException;
import com.example.marker.exception.UserAlreadyExistsException;
import com.example.marker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @DisplayName("회원가입 - 성공")
    @Test
    void signup_Success() {
        // given
        AuthSignupRequest request = new AuthSignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // when
        Long userId = authService.signup(request);

        // then
        assertThat(userId).isNotNull();
        User savedUser = userRepository.findById(userId).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @DisplayName("회원가입 - 비밀번호 암호화 확인")
    @Test
    void signup_PasswordIsEncrypted() {
        // given
        AuthSignupRequest request = new AuthSignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("plainPassword123");

        // when
        Long userId = authService.signup(request);

        // then
        User savedUser = userRepository.findById(userId).orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("plainPassword123");
        assertThat(passwordEncoder.matches("plainPassword123", savedUser.getPassword())).isTrue();
    }

    @DisplayName("회원가입 - 실패 (중복된 이메일)")
    @Test
    void signup_Fail_DuplicateEmail() {
        // given
        AuthSignupRequest request = new AuthSignupRequest();
        request.setEmail("duplicate@example.com");
        request.setPassword("password123");
        authService.signup(request);

        // when & then
        AuthSignupRequest duplicateRequest = new AuthSignupRequest();
        duplicateRequest.setEmail("duplicate@example.com");
        duplicateRequest.setPassword("anotherPassword");

        assertThatThrownBy(() -> authService.signup(duplicateRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("duplicate@example.com");
    }

    @DisplayName("로그인 - 성공")
    @Test
    void login_Success() {
        // given
        AuthSignupRequest signupRequest = new AuthSignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        authService.signup(signupRequest);

        AuthLoginRequest loginRequest = new AuthLoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // when
        AuthLoginResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).isNotEmpty();
    }

    @DisplayName("로그인 - 유효하지 않은 이메일")
    @Test
    void login_InvalidEmail_ThrowsException() {
        // given
        AuthLoginRequest request = new AuthLoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password");

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @DisplayName("로그인 - 잘못된 비밀번호")
    @Test
    void login_InvalidPassword_ThrowsException() {
        // given
        AuthSignupRequest signupRequest = new AuthSignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("correctPassword");
        authService.signup(signupRequest);

        AuthLoginRequest loginRequest = new AuthLoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongPassword");

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @DisplayName("로그아웃 - 성공 (서버 측 로직 없음)")
    @Test
    void logout_Success() {
        // when & then
        // JWT 기반이므로 서버 측 로그아웃은 특별한 로직이 없음
        // 예외가 발생하지 않으면 성공
        authService.logout();
    }
}