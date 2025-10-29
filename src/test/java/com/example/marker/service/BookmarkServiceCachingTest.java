package com.example.marker.service;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.User;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import com.example.marker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class BookmarkServiceCachingTest {

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private UserRepository userRepository;

    // @SpyBean: 실제 BookmarkRepository Bean을 사용하면서도,
    // 특정 메소드의 호출 횟수 등을 추적할 수 있게 해줍니다.
    @SpyBean
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private CacheManager cacheManager;

    private User user;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성 및 인증 정보 설정
        user = userRepository.save(User.builder()
                .email("cache_test@example.com")
                .password("password")
                .build());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                String.valueOf(user.getId()),
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 각 테스트 시작 전 캐시 초기화
        cacheManager.getCache("bookmark").clear();
    }

    @DisplayName("@Cacheable: 북마크 상세 조회 시 캐싱 적용")
    @Test
    void getBookmarkById_isCacheable() {
        // given
        Bookmark bookmark = bookmarkRepository.save(Bookmark.builder().title("Cache Test").url("...").user(user).build());

        // when
        // 1. 첫 번째 호출 (DB 조회 발생, 캐시에 저장)
        bookmarkService.getBookmarkById(bookmark.getId());

        // 2. 두 번째 호출 (캐시에서 조회)
        bookmarkService.getBookmarkById(bookmark.getId());

        // then
        // findByIdWithTags 메소드가 총 1번만 호출되었는지 검증
        verify(bookmarkRepository, times(1)).findByIdWithTags(bookmark.getId());
    }

    @DisplayName("@CachePut: 북마크 수정 시 캐시 갱신")
    @Test
    void updateBookmark_updatesCache() {
        // given
        Bookmark bookmark = bookmarkRepository.save(Bookmark.builder().title("Original Title").url("...").user(user).build());

        // 1. 캐시에 데이터 저장
        bookmarkService.getBookmarkById(bookmark.getId());

        // when
        // 2. 북마크 수정 (DB 업데이트 및 캐시 갱신)
        BookmarkUpdateRequest updateRequest = new BookmarkUpdateRequest("Updated Title", "...", "...", List.of());
        bookmarkService.updateBookmark(bookmark.getId(), updateRequest);

        // 3. 수정 후 다시 조회 (갱신된 캐시에서 조회)
        bookmarkService.getBookmarkById(bookmark.getId());

        // then
        // 첫 번째 조회(1) + 수정 시 내부 조회(1) = 총 2번
        // 수정 후 다시 조회할 때는 캐시를 사용하므로 추가 호출이 없어야 함
        verify(bookmarkRepository, times(2)).findByIdWithTags(bookmark.getId());
    }

    @DisplayName("@CacheEvict: 북마크 삭제 시 캐시 제거")
    @Test
    void deleteBookmark_evictsCache() {
        // given
        Bookmark bookmark = bookmarkRepository.save(Bookmark.builder().title("To be deleted").url("...").user(user).build());

        // 1. 캐시에 데이터 저장
        bookmarkService.getBookmarkById(bookmark.getId());

        // when
        // 2. 북마크 삭제 (DB 삭제 및 캐시 제거)
        bookmarkService.deleteBookmark(bookmark.getId());

        // 3. 삭제 후 다시 조회 시도 (캐시가 없으므로 DB 조회 시도)
        try {
            bookmarkService.getBookmarkById(bookmark.getId());
        } catch (Exception e) {
            // BookmarkNotFoundException이 발생하는 것이 정상
        }

        // then
        // 첫 번째 조회(1) + 삭제 시 내부 조회(1) + 삭제 후 재조회(1) = 총 3번
        verify(bookmarkRepository, times(3)).findByIdWithTags(bookmark.getId());
    }
}