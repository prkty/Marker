package com.example.marker.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.User;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.exception.BookmarkNotFoundException;
import com.example.marker.exception.UnauthorizedBookmarkAccessException;
import com.example.marker.repository.BookmarkRepository;
import com.example.marker.repository.UserRepository;

/**
 * BookmarkService에 대한 단위 테스트 클래스.
 * Mockito를 사용하여 Repository의 의존성을 격리하고 서비스 로직만 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    // 테스트 대상 클래스. @Mock으로 생성된 객체들이 이 클래스에 주입됩니다.
    @InjectMocks
    private BookmarkService bookmarkService;

    /**
     * @Mock: 가짜(Mock) 객체를 생성합니다.
     * BookmarkService는 BookmarkRepository에 의존하므로, 실제 데이터베이스와 상호작용하지 않도록
     * 가짜 Repository를 만들어 사용합니다.
     */
    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookmarkFinder bookmarkFinder;

    @Mock
    private BookmarkUpdater bookmarkUpdater;
    
    @Mock
    private BookmarkTagService bookmarkTagService;

    private User user;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 객체 생성
        user = User.builder().email("test@example.com").password("password").build();
        // User 객체에 ID를 강제로 설정합니다.
        ReflectionTestUtils.setField(user, "id", userId);

        // SecurityContextHolder 모의 설정
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // authentication.getName()이 사용자 ID 문자열을 반환하도록 직접 모의(Mocking)합니다.
        when(authentication.getName()).thenReturn(String.valueOf(userId));
        when(authentication.isAuthenticated()).thenReturn(true);
    }

    @DisplayName("북마크 생성 - 성공")
    @Test
    void createBookmark_Success() {
        // given
        final BookmarkCreateRequest request = new BookmarkCreateRequest("Google", "https://www.google.com", "Search Engine", List.of("검색", "IT"));

        // 사용자 조회 Mocking
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // repository.save()가 호출될 때의 가짜 동작 정의
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(invocation -> {
            Bookmark savedBookmark = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedBookmark, "id", 1L); // ID 설정
            return savedBookmark;
        });

        // when
        final BookmarkResponse response = bookmarkService.createBookmark(request);

        // then
        // 반환된 응답 값이 예상과 일치하는지 검증
        assertThat(response.getTitle()).isEqualTo("Google");
        assertThat(response.getUrl()).isEqualTo("https://www.google.com");
        assertThat(response.getMemo()).isEqualTo("Search Engine");

        // repository.save()가 한 번만 호출되었는지 검증
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
        // bookmarkTagService의 메서드가 올바르게 호출되었는지 검증
        verify(bookmarkTagService, times(1)).associateTagsWithBookmark(any(Bookmark.class), eq(request.getTags()));
    }

    @DisplayName("북마크 전체 조회 - 성공")
    @Test
    void getAllBookmarks_Success() {
        // given
        final Bookmark bookmark1 = Bookmark.builder().id(1L).title("Google").url("https://www.google.com").user(user).build();
        final Bookmark bookmark2 = Bookmark.builder().id(2L).title("Naver").url("https://www.naver.com").user(user).build();
        final List<Bookmark> bookmarks = List.of(bookmark1, bookmark2);
        final PageRequest pageable = PageRequest.of(0, 5);

        when(bookmarkRepository.findAllByUserId(userId, pageable)).thenReturn(new PageImpl<>(bookmarks, pageable, bookmarks.size()));

        // when
        final Page<BookmarkResponse> responses = bookmarkService.getAllBookmarks(pageable);

        // then
        assertThat(responses.getTotalElements()).isEqualTo(2);
        assertThat(responses.getContent())
                .extracting("title", "url")
                .containsExactlyInAnyOrder(
                        tuple("Google", "https://www.google.com"),
                        tuple("Naver", "https://www.naver.com")
                );

        verify(bookmarkRepository, times(1)).findAllByUserId(userId, pageable);
    }

    @DisplayName("북마크 상세 조회 - 성공")
    @Test
    void getBookmarkById_Success() {
        // given
        final Bookmark bookmark = Bookmark.builder().id(1L).title("Google").url("https://www.google.com").memo("memo").user(user).build();
        // BookmarkFinder가 Bookmark 엔티티를 반환하도록 모의(Mocking)합니다.
        when(bookmarkFinder.findBookmarkById(1L, userId)).thenReturn(bookmark);

        // when
        final BookmarkResponse response = bookmarkService.getBookmarkById(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Google");

        // bookmarkFinder.findBookmarkById가 정확한 인자로 1번 호출되었는지 검증
        verify(bookmarkFinder, times(1)).findBookmarkById(1L, userId);
    }

    @DisplayName("북마크 상세 조회 - 실패 (존재하지 않는 ID)")
    @Test
    void getBookmarkById_Fail_NotFound() {
        // given
        // 스파이 객체의 메소드가 예외를 던지도록 설정합니다.
        when(bookmarkFinder.findBookmarkById(99L, userId)).thenThrow(new BookmarkNotFoundException(99L));

        // when & then
        assertThatThrownBy(() -> bookmarkService.getBookmarkById(99L))
                .isInstanceOf(BookmarkNotFoundException.class)
                .hasMessage("Bookmark not found with id: 99");

        verify(bookmarkFinder, times(1)).findBookmarkById(99L, userId);
    }

    @DisplayName("북마크 수정 - 성공")
    @Test
    void updateBookmark_Success() {
        // given
        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo", List.of("Updated Tag"));
        final Bookmark updatedBookmark = Bookmark.builder()
                .id(1L)
                .title(request.getTitle())
                .url(request.getUrl())
                .memo(request.getMemo())
                .user(user)
                .build();

        // BookmarkUpdater가 수정된 Bookmark 엔티티를 반환하도록 모의(Mocking)합니다.
        when(bookmarkUpdater.updateBookmarkAndCache(anyLong(), eq(1L), eq(request))).thenReturn(updatedBookmark);

        // when
        // 이 라인이 누락되면 UnnecessaryStubbingException이 발생합니다.
        final BookmarkResponse response = bookmarkService.updateBookmark(1L, request);

        // then
        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getUrl()).isEqualTo("https://updated.com");

        // bookmarkUpdater.updateBookmarkAndCache가 정확한 인자로 1번 호출되었는지 검증
        verify(bookmarkUpdater, times(1)).updateBookmarkAndCache(anyLong(), eq(1L), eq(request));
    }

    @DisplayName("북마크 삭제 - 성공")
    @Test
    void deleteBookmark_Success() {
        // given (사전 조건 설정)
        // 1. bookmarkUpdater.deleteBookmarkAndEvictCache가 호출될 때 반환할 가짜 Bookmark 객체를 준비합니다.
        Bookmark fakeBookmark = Bookmark.builder().id(1L).user(user).build();

        // 2. bookmarkUpdater가 호출되면 null 대신 위에서 만든 가짜 객체를 반환하도록 설정합니다.
        //    이것이 없으면 bookmarkRepository.delete(null)이 호출되어 오류가 발생합니다.
        when(bookmarkUpdater.deleteBookmarkAndEvictCache(userId, 1L)).thenReturn(fakeBookmark);

        // when (테스트 대상 메서드 실행)
        // 3. 실제 테스트 대상 메서드를 호출합니다.
        bookmarkService.deleteBookmark(1L, userId);

        // then (결과 검증)
        // 4. BookmarkService가 의존하는 객체들의 메서드를 올바르게 호출했는지 검증합니다.
        verify(bookmarkUpdater, times(1)).deleteBookmarkAndEvictCache(userId, 1L);
        verify(bookmarkRepository, times(1)).delete(fakeBookmark);
    }

    @DisplayName("태그로 북마크 조회 - 성공")
    @Test
    void getBookmarksByTag_Success() {
        // given
        String tagName = "개발";
        Bookmark bookmark1 = Bookmark.builder().id(1L).title("Spring Blog").url("...").user(user).build();
        Bookmark bookmark2 = Bookmark.builder().id(2L).title("JPA Docs").url("...").user(user).build();
        PageRequest pageable = PageRequest.of(0, 5);
        when(bookmarkRepository.findByUserIdAndTagName(userId, tagName, pageable)).thenReturn(new PageImpl<>(List.of(bookmark1, bookmark2), pageable, 2));

        // when
        Page<BookmarkResponse> responses = bookmarkService.getBookmarksByTag(tagName, pageable);

        // then
        assertThat(responses.getTotalElements()).isEqualTo(2);
        assertThat(responses.getContent()).extracting("title").containsExactly("Spring Blog", "JPA Docs");
        verify(bookmarkRepository, times(1)).findByUserIdAndTagName(userId, tagName, pageable);
    }

    @DisplayName("키워드(제목 또는 URL)로 북마크 검색 - 성공")
    @Test
    void searchBookmarks_Success() {
        // given
        String keyword = "spring";
        Bookmark bookmark1 = Bookmark.builder().id(1L).title("Spring Blog").url("...").user(user).build();
        Bookmark bookmark2 = Bookmark.builder().id(2L).title("Another Spring Guide").url("...").user(user).build();
        PageRequest pageable = PageRequest.of(0, 5);
        when(bookmarkRepository.findByUserIdAndKeyword(userId, keyword, pageable)).thenReturn(new PageImpl<>(List.of(bookmark1, bookmark2), pageable, 2));

        // when
        Page<BookmarkResponse> responses = bookmarkService.searchBookmarks(keyword, pageable);

        // then
        assertThat(responses.getTotalElements()).isEqualTo(2);
        assertThat(responses.getContent()).extracting("title").containsExactly("Spring Blog", "Another Spring Guide");
        verify(bookmarkRepository, times(1)).findByUserIdAndKeyword(userId, keyword, pageable);
    }

    @DisplayName("다른 사용자의 북마크 접근 - 실패 (인가 실패)")
    @Test
    void accessOthersBookmark_Fail_Forbidden() {
        // given
        // 다른 사용자(userId=2)의 북마크
        User anotherUser = User.builder().email("another@user.com").password("password").build();
        ReflectionTestUtils.setField(anotherUser, "id", 2L); // 다른 사용자의 ID 설정
        Bookmark othersBookmark = Bookmark.builder().id(2L).title("Another's Bookmark").url("...").user(anotherUser).build();
        
        // BookmarkFinder가 권한 없음 예외를 던지도록 설정합니다.
        when(bookmarkFinder.findBookmarkById(2L, userId)).thenThrow(new UnauthorizedBookmarkAccessException(2L, userId));

        // when & then
        // 현재 로그인한 사용자(userId=1)가 다른 사용자(userId=2)의 북마크에 접근 시도
        assertThatThrownBy(() -> bookmarkService.getBookmarkById(2L))
                .isInstanceOf(UnauthorizedBookmarkAccessException.class);
        verify(bookmarkFinder, times(1)).findBookmarkById(2L, userId);
    }
}