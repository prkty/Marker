package com.example.marker.controller;

import com.example.marker.dto.AuthLoginRequest;
import com.example.marker.dto.AuthSignupRequest;
import com.example.marker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    @DisplayName("회원가입 API - 성공")
    @Test
    void signup_Success() throws Exception {
        // given
        AuthSignupRequest request = new AuthSignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @DisplayName("회원가입 API - 실패 (중복된 이메일)")
    @Test
    void signup_Fail_DuplicateEmail() throws Exception {
        // given
        // 먼저 사용자를 하나 생성
        AuthSignupRequest initialRequest = new AuthSignupRequest();
        initialRequest.setEmail("test@example.com");
        initialRequest.setPassword("password123");
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialRequest)));

        // 동일한 이메일로 다시 회원가입 시도
        AuthSignupRequest duplicateRequest = new AuthSignupRequest();
        duplicateRequest.setEmail("test@example.com");
        duplicateRequest.setPassword("anotherPassword");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict()); // 409 Conflict
    }

    @DisplayName("로그인 API - 성공")
    @Test
    void login_Success() throws Exception {
        // given
        // 회원가입
        AuthSignupRequest signupRequest = new AuthSignupRequest();
        signupRequest.setEmail("test@example.com");
        signupRequest.setPassword("password123");
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(signupRequest)));

        // 로그인 요청
        AuthLoginRequest loginRequest = new AuthLoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists()); // 토큰이 존재하는지 확인
    }

    @DisplayName("로그인 API - 실패 (잘못된 비밀번호)")
    @Test
    void login_Fail_InvalidCredentials() throws Exception {
        // given
        AuthLoginRequest loginRequest = new AuthLoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongPassword");

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()); // 401 Unauthorized
    }
}