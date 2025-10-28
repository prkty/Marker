package com.example.marker.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.BookmarkTag;
import com.example.marker.domain.Tag;
import com.example.marker.domain.User;
import com.example.marker.dto.AuthLoginRequest;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import com.example.marker.repository.TagRepository;
import com.example.marker.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * BookmarkController에 대한 통합 테스트 클래스.
 * MockMvc를 사용하여 실제 HTTP 요청을 시뮬레이션하고, API 엔드포인트의 동작을 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookmarkControllerTest {

    // 컨트롤러 API에 HTTP 요청을 보내기 위한 MockMvc 객체
    @Autowired
    private MockMvc mockMvc;

    // Java 객체를 JSON으로 변환하기 위한 ObjectMapper 객체
    @Autowired
    private ObjectMapper objectMapper;

    // 테스트 데이터 준비 및 결과 검증을 위한 Repository
    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    private String userToken;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 실행 전, 사용자 생성 및 토큰 발급
        userRepository.deleteAll();
        bookmarkRepository.deleteAll();

        // 회원가입
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"testuser@example.com\", \"password\":\"password123\"}"));

        // 로그인 후 토큰 획득
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"testuser@example.com\", \"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();

        userToken = objectMapper.readTree(response).get("token").asText();

        // 생성된 사용자 정보를 가져와서 user 필드에 할당
        user = userRepository.findByEmail("testuser@example.com").orElseThrow();
    }

    @DisplayName("북마크 생성 API - 성공")
    @Test
    void createBookmark_Success() throws Exception {
        // given
        final BookmarkCreateRequest request = new BookmarkCreateRequest("Test Title", "https://test.com", "Test Memo", List.of("테스트", "Java"));
        final String jsonRequest = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/bookmarks")
                        .header("Authorization", "Bearer " + userToken) // 인증 헤더 추가
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.url").value("https://test.com"))
                .andExpect(jsonPath("$.tags[?(@ == '테스트')]").exists())
                .andExpect(jsonPath("$.tags[?(@ == 'Java')]").exists());
    }

    @DisplayName("북마크 전체 조회 API - 성공")
    @Test
    void getAllBookmarks_Success() throws Exception {
        // given
        // 현재 로그인한 사용자의 북마크만 생성
        bookmarkRepository.save(Bookmark.builder().title("Google").url("https://www.google.com").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("Naver").url("https://www.naver.com").user(user).build());

        // when & then
        mockMvc.perform(get("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Google"))
                .andExpect(jsonPath("$.content[1].title").value("Naver")); // 순서가 보장되지 않으므로 더 나은 검증 방법이 필요할 수 있음
    }

    @DisplayName("북마크 생성 API - 실패 (유효성 검증 실패)")
    @Test
    void createBookmark_Fail_Validation() throws Exception {
        // given
        final BookmarkCreateRequest request = new BookmarkCreateRequest("", "", "Test Memo", List.of("테스트")); // 제목과 URL이 비어있음
        final String jsonRequest = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.errors").isArray()); // 에러 필드 존재 확인
    }

    @DisplayName("태그로 북마크 조회 API - 성공")
    @Test
    void getBookmarks_ByTag_Success() throws Exception {
        // given
        Tag devTag = tagRepository.save(Tag.builder().name("개발").build());
        Tag newsTag = tagRepository.save(Tag.builder().name("뉴스").build());

        Bookmark bookmark1 = Bookmark.builder().title("Spring Blog").url("...").user(user).build();
        bookmark1.addBookmarkTag(BookmarkTag.builder().tag(devTag).build());
        bookmarkRepository.save(bookmark1);

        Bookmark bookmark2 = Bookmark.builder().title("Naver News").url("...").user(user).build();
        bookmark2.addBookmarkTag(BookmarkTag.builder().tag(newsTag).build());
        bookmarkRepository.save(bookmark2);

        // when & then
        mockMvc.perform(get("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .param("tag", "개발")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Spring Blog"));
    }

    @DisplayName("키워드(제목 또는 URL)로 북마크 검색 API - 성공")
    @Test
    void getBookmarks_ByKeyword_Success() throws Exception {
        // given
        bookmarkRepository.save(Bookmark.builder().title("Spring Boot Guide").url("https://spring.io").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("Naver News").url("https://news.naver.com").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("About Java").url("https://www.java.com").user(user).build());

        // when & then
        // 'java' 키워드로 검색
        mockMvc.perform(get("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .param("keyword", "java")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(json->jsonPath("$.content[0].title").value("About Java"));
    }


    @DisplayName("북마크 상세 조회 API - 성공")
    @Test
    void getBookmarkById_Success() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Google")
                .url("https://www.google.com").user(user)
                .build());

        // when & then
        mockMvc.perform(get("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedBookmark.getId()))
                .andExpect(jsonPath("$.title").value("Google"))
                .andExpect(jsonPath("$.url").value("https://www.google.com"));
    }

    @DisplayName("북마크 상세 조회 API - 실패 (존재하지 않는 ID)")
    @Test
    void getBookmarkById_Fail_NotFound() throws Exception {
        // given
        Long nonExistentId = 999L;

        // when & then
        mockMvc.perform(get("/bookmarks/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isNotFound()); // 404 Not Found
    }

    @DisplayName("북마크 수정 API - 성공")
    @Test
    void updateBookmark_Success() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Original Title")
                .url("https://original.com")
                .memo("Original Memo").user(user)
                .build());

        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo", List.of("수정된태그"));
        final String jsonRequest = objectMapper.writeValueAsString(request);

        // when
        mockMvc.perform(put("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.memo").value("Updated Memo"))
                .andExpect(jsonPath("$.tags[0]").value("수정된태그"));

        // then
        // 데이터베이스의 실제 데이터가 변경되었는지 확인
        final Bookmark updatedBookmark = bookmarkRepository.findById(savedBookmark.getId()).get();
        assertThat(updatedBookmark.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedBookmark.getMemo()).isEqualTo("Updated Memo");
        assertThat(updatedBookmark.getBookmarkTags().get(0).getTag().getName()).isEqualTo("수정된태그");
    }

    @DisplayName("북마크 수정 API - 실패 (존재하지 않는 ID)")
    @Test
    void updateBookmark_Fail_NotFound() throws Exception {
        // given
        Long nonExistentId = 999L;
        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo", List.of("수정된태그"));
        final String jsonRequest = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/bookmarks/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound()); // 404 Not Found
    }

    @DisplayName("북마크 수정 API - 실패 (유효성 검증 실패)")
    @Test
    void updateBookmark_Fail_Validation() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder().title("Original").url("https://original.com").user(user).build());
        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("", "invalid-url", "Updated Memo", List.of()); // 제목 비어있고 URL 형식 오류
        final String jsonRequest = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(put("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'url')]").exists());
    }

    @DisplayName("북마크 삭제 API - 성공")
    @Test
    void deleteBookmark_Success() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Google")
                .url("https://www.google.com").user(user)
                .build());

        // when & then
        mockMvc.perform(delete("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // 데이터베이스에서 실제 데이터가 삭제되었는지 확인
        assertThat(bookmarkRepository.findById(savedBookmark.getId())).isEmpty();
    }

    @DisplayName("북마크 삭제 API - 실패 (존재하지 않는 ID)")
    @Test
    void deleteBookmark_Fail_NotFound() throws Exception {
        // given
        Long nonExistentId = 999L;

        // when & then
        mockMvc.perform(delete("/bookmarks/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound()); // 404 Not Found
    }

    @DisplayName("북마크 API 접근 - 실패 (인증되지 않은 사용자)")
    @Test
    void accessBookmarkApi_Fail_Unauthorized() throws Exception {
        // given
        // 토큰 없이 요청

        // when & then
        mockMvc.perform(get("/bookmarks"))
                .andExpect(status().isForbidden()); // Spring Security는 기본적으로 403 Forbidden 반환

        mockMvc.perform(post("/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @DisplayName("다른 사용자의 북마크 접근 - 실패 (인가 실패)")
    @Test
    void accessOthersBookmark_Fail_Forbidden() throws Exception {
        // given
        // userA (테스트 기본 사용자)가 북마크 생성
        Bookmark userABookmark = bookmarkRepository.save(Bookmark.builder().title("UserA's Bookmark").url("...").user(user).build());

        // userB 생성 및 토큰 발급
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"userB@example.com\", \"password\":\"password123\"}"));

        String userBResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"userB@example.com\", \"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        String userBToken = objectMapper.readTree(userBResponse).get("token").asText();

        // when & then
        // userB가 userA의 북마크에 접근 시도
        mockMvc.perform(get("/bookmarks/{id}", userABookmark.getId())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden()); // 403 Forbidden

        mockMvc.perform(delete("/bookmarks/{id}", userABookmark.getId())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());
    }
}
