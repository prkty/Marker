package com.example.marker.controller;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
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
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
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

    @DisplayName("북마크 생성 API - 성공")
    @Test
    void createBookmark_Success() throws Exception {
        // given
        final BookmarkCreateRequest request = new BookmarkCreateRequest("Test Title", "https://test.com", "Test Memo");
        final String jsonRequest = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Test Title"))
                .andExpect(jsonPath("$.url").value("https://test.com"));
    }

    @DisplayName("북마크 전체 조회 API - 성공")
    @Test
    void getAllBookmarks_Success() throws Exception {
        // given
        bookmarkRepository.saveAll(List.of(
                Bookmark.builder().title("Google").url("https://www.google.com").build(),
                Bookmark.builder().title("Naver").url("https://www.naver.com").build()
        ));

        // when & then
        mockMvc.perform(get("/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Google"))
                .andExpect(jsonPath("$[1].title").value("Naver"));
    }

    @DisplayName("북마크 상세 조회 API - 성공")
    @Test
    void getBookmarkById_Success() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Google")
                .url("https://www.google.com")
                .build());

        // when & then
        mockMvc.perform(get("/bookmarks/{id}", savedBookmark.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedBookmark.getId()))
                .andExpect(jsonPath("$.title").value("Google"))
                .andExpect(jsonPath("$.url").value("https://www.google.com"));
    }

    @DisplayName("북마크 수정 API - 성공")
    @Test
    void updateBookmark_Success() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Original Title")
                .url("https://original.com")
                .memo("Original Memo")
                .build());

        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo");
        final String jsonRequest = objectMapper.writeValueAsString(request);

        // when
        mockMvc.perform(put("/bookmarks/{id}", savedBookmark.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.memo").value("Updated Memo"));

        // then
        // 데이터베이스의 실제 데이터가 변경되었는지 확인
        final Bookmark updatedBookmark = bookmarkRepository.findById(savedBookmark.getId()).get();
        assertThat(updatedBookmark.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedBookmark.getMemo()).isEqualTo("Updated Memo");
    }

    @DisplayName("북마크 삭제 API - 성공")
    @Test
    void deleteBookmark_Success() throws Exception {
        // given
        final Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .title("Google")
                .url("https://www.google.com")
                .build());

        // when & then
        mockMvc.perform(delete("/bookmarks/{id}", savedBookmark.getId()))
                .andExpect(status().isNoContent());

        // 데이터베이스에서 실제 데이터가 삭제되었는지 확인
        assertThat(bookmarkRepository.findById(savedBookmark.getId())).isEmpty();
    }
}
