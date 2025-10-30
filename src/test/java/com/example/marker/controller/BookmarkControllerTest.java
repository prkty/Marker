package com.example.marker.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        userRepository.deleteAll();
        bookmarkRepository.deleteAll();

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"testuser@example.com\", \"password\":\"password123\"}"));

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"testuser@example.com\", \"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();

        userToken = objectMapper.readTree(response).get("token").asText();
        user = userRepository.findByEmail("testuser@example.com").orElseThrow();
    }

    @DisplayName("북마크 생성 API - 성공")
    @Test
    void createBookmark_Success() throws Exception {
        final BookmarkCreateRequest request = new BookmarkCreateRequest("Test Title", "https://test.com", "Test Memo", List.of("테스트", "Java"));
        final String jsonRequest = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
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
        bookmarkRepository.save(Bookmark.builder().title("Google").url("https://www.google.com").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("Naver").url("https://www.naver.com").user(user).build());

        mockMvc.perform(get("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                // 순서에 상관없이 title 필드의 값만 검증
                .andExpect(jsonPath("$.content[*].title", containsInAnyOrder("Google", "Naver")));
    }

    @DisplayName("북마크 생성 API - 실패 (유효성 검증 실패)")
    @Test
    void createBookmark_Fail_Validation() throws Exception {
        final BookmarkCreateRequest request = new BookmarkCreateRequest("", "", "Test Memo", List.of("테스트"));
        final String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @DisplayName("태그로 북마크 조회 API - 성공")
    @Test
    void getBookmarks_ByTag_Success() throws Exception {
        Tag devTag = tagRepository.save(Tag.builder().name("개발").build());
        Tag newsTag = tagRepository.save(Tag.builder().name("뉴스").build());

        Bookmark bookmark1 = Bookmark.builder().title("Spring Blog").url("...").user(user).build();
        bookmark1.addBookmarkTag(BookmarkTag.builder().tag(devTag).build());
        bookmarkRepository.save(bookmark1);

        Bookmark bookmark2 = Bookmark.builder().title("Naver News").url("...").user(user).build();
        bookmark2.addBookmarkTag(BookmarkTag.builder().tag(newsTag).build());
        bookmarkRepository.save(bookmark2);

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
        bookmarkRepository.save(Bookmark.builder().title("Spring Boot Guide").url("https://spring.io").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("Naver News").url("https://news.naver.com").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("About Java").url("https://www.java.com").user(user).build());

        mockMvc.perform(get("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .param("keyword", "java")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("About Java"));
    }

    @DisplayName("북마크 상세 조회 API - 성공")
    @Test
    void getBookmarkById_Success() throws Exception {
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Google")
                .url("https://www.google.com").user(user)
                .build());

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
        Long nonExistentId = 999L;

        mockMvc.perform(get("/bookmarks/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + userToken)
                )
                .andExpect(status().isNotFound());
    }

    @DisplayName("북마크 수정 API - 성공")
    @Test
    void updateBookmark_Success() throws Exception {
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Original Title")
                .url("https://original.com")
                .memo("Original Memo").user(user)
                .build());

        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo", List.of("수정된태그"));
        final String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.memo").value("Updated Memo"))
                .andExpect(jsonPath("$.tags[0]").value("수정된태그"));

        final Bookmark updatedBookmark = bookmarkRepository.findById(savedBookmark.getId()).get();
        assertThat(updatedBookmark.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedBookmark.getMemo()).isEqualTo("Updated Memo");
        assertThat(updatedBookmark.getBookmarkTags().get(0).getTag().getName()).isEqualTo("수정된태그");
    }

    @DisplayName("북마크 수정 API - 실패 (존재하지 않는 ID)")
    @Test
    void updateBookmark_Fail_NotFound() throws Exception {
        Long nonExistentId = 999L;
        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo", List.of("수정된태그"));
        final String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/bookmarks/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound());
    }

    @DisplayName("북마크 수정 API - 실패 (유효성 검증 실패)")
    @Test
    void updateBookmark_Fail_Validation() throws Exception {
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder().title("Original").url("https://original.com").user(user).build());
        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("", "invalid-url", "Updated Memo", List.of());
        final String jsonRequest = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'url')]").exists());
    }

    @DisplayName("북마크 삭제 API - 성공")
    @Test
    void deleteBookmark_Success() throws Exception {
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Google")
                .url("https://www.google.com").user(user)
                .build());

        mockMvc.perform(delete("/bookmarks/{id}", savedBookmark.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        assertThat(bookmarkRepository.findById(savedBookmark.getId())).isEmpty();
    }

    @DisplayName("북마크 삭제 API - 실패 (존재하지 않는 ID)")
    @Test
    void deleteBookmark_Fail_NotFound() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(delete("/bookmarks/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @DisplayName("북마크 API 접근 - 실패 (인증되지 않은 사용자)")
    @Test
    void accessBookmarkApi_Fail_Unauthorized() throws Exception {
        mockMvc.perform(get("/bookmarks"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @DisplayName("다른 사용자의 북마크 접근 - 실패 (인가 실패)")
    @Test
    void accessOthersBookmark_Fail_Forbidden() throws Exception {
        Bookmark userABookmark = bookmarkRepository.save(Bookmark.builder().title("UserA's Bookmark").url("...").user(user).build());

        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"userB@example.com\", \"password\":\"password123\"}"));

        String userBResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"userB@example.com\", \"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        String userBToken = objectMapper.readTree(userBResponse).get("token").asText();

        mockMvc.perform(get("/bookmarks/{id}", userABookmark.getId())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/bookmarks/{id}", userABookmark.getId())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isForbidden());
    }

    // ========== 추가된 테스트 케이스 ==========

    @DisplayName("페이지네이션 - 정렬 옵션 테스트")
    @Test
    void getBookmarks_WithSorting_Success() throws Exception {
        bookmarkRepository.save(Bookmark.builder()
                .title("Old Bookmark")
                .url("https://old.com")
                .user(user)
                .build());
        
        Thread.sleep(100); // createdAt 차이를 두기 위함
        
        bookmarkRepository.save(Bookmark.builder()
                .title("New Bookmark")
                .url("https://new.com")
                .user(user)
                .build());

        mockMvc.perform(get("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("New Bookmark"))
                .andExpect(jsonPath("$.content[1].title").value("Old Bookmark"));
    }

    @DisplayName("여러 태그가 있는 북마크 생성 및 조회")
    @Test
    void createAndRetrieveBookmark_WithMultipleTags_Success() throws Exception {
        BookmarkCreateRequest request = new BookmarkCreateRequest(
                "Tech Article",
                "https://tech.com",
                "Good article",
                List.of("Java", "Spring", "Backend")
        );

        String response = mockMvc.perform(post("/bookmarks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookmarkId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/bookmarks/{id}", bookmarkId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(3)))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("Java", "Spring", "Backend")));
    }
}