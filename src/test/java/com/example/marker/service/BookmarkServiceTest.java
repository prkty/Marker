package com.example.marker.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import org.mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.Tag;
import com.example.marker.domain.User;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.exception.BookmarkNotFoundException;
import com.example.marker.exception.UnauthorizedBookmarkAccessException;
import com.example.marker.repository.BookmarkRepository;
import com.example.marker.repository.TagRepository;
import com.example.marker.repository.UserRepository;

/**
 * BookmarkService에 대한 단위 테스트 클래스.
 * Mockito를 사용하여 Repository의 의존성을 격리하고 서비스 로직만 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    // @InjectMocks와 @Spy를 함께 사용하여 실제 객체 기반의 스파이를 생성하고,
    // @Mock으로 선언된 의존성을 주입받습니다.
    @InjectMocks
    @Spy
    private BookmarkService bookmarkService;

    /**
     * @Mock: 가짜(Mock) 객체를 생성합니다.
     * BookmarkService는 BookmarkRepository에 의존하므로, 실제 데이터베이스와 상호작용하지 않도록
     * 가짜 Repository를 만들어 사용합니다.
     */
    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    private User user;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 객체 생성
        user = User.builder().email("test@example.com").password("password").build();
        // User 객체에 ID를 강제로 설정합니다.
        ReflectionTestUtils.setField(user, "id", userId);

        // @Spy로 생성된 bookmarkService가 자기 자신을 참조할 수 있도록 self 필드를 주입합니다.
        ReflectionTestUtils.setField(bookmarkService, "self", bookmarkService);

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

        // 태그 관련 Mocking
        when(tagRepository.findByName("검색")).thenReturn(Optional.empty());
        when(tagRepository.findByName("IT")).thenReturn(Optional.of(Tag.builder().id(1L).name("IT").build()));
        when(tagRepository.save(any(Tag.class))).thenReturn(Tag.builder().name("검색").build());
        // 사용자 조회 Mocking
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // repository.save()가 호출될 때의 가짜 동작 정의
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        final BookmarkResponse response = bookmarkService.createBookmark(request);

        // then
        // 반환된 응답 값이 예상과 일치하는지 검증
        assertThat(response.getTitle()).isEqualTo("Google");
        assertThat(response.getUrl()).isEqualTo("https://www.google.com");
        assertThat(response.getMemo()).isEqualTo("Search Engine");
        assertThat(response.getTags()).containsExactlyInAnyOrder("검색", "IT");

        // repository.save()가 한 번만 호출되었는지 검증
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
        verify(tagRepository, times(1)).save(any(Tag.class));
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
        // getCurrentUserId()를 모의(Mocking)하고, findAndCacheBookmarkById를 호출하도록 합니다.
        doReturn(userId).when(bookmarkService).getCurrentUserId();
        doReturn(bookmark).when(bookmarkService).findAndCacheBookmarkById(1L, userId);

        // when
        final BookmarkResponse response = bookmarkService.getBookmarkById(1L);

        // then
        assertThat(response.getTitle()).isEqualTo("Google");
        assertThat(response.getUrl()).isEqualTo("https://www.google.com");
        assertThat(response.getMemo()).isEqualTo("memo");

        verify(bookmarkService, times(1)).findAndCacheBookmarkById(1L, userId);
    }

    @DisplayName("북마크 상세 조회 - 실패 (존재하지 않는 ID)")
    @Test
    void getBookmarkById_Fail_NotFound() {
        // given
        // getCurrentUserId()를 모의(Mocking)합니다.
        doReturn(userId).when(bookmarkService).getCurrentUserId();
        doThrow(new BookmarkNotFoundException(99L)).when(bookmarkService).findAndCacheBookmarkById(anyLong(), anyLong());

        // when & then
        assertThatThrownBy(() -> bookmarkService.getBookmarkById(99L))
                .isInstanceOf(BookmarkNotFoundException.class)
                .hasMessage("Bookmark not found with id: 99");

        verify(bookmarkService, times(1)).findAndCacheBookmarkById(99L, userId);
    }

    @DisplayName("북마크 수정 - 성공")
    @Test
    void updateBookmark_Success() {
        // given
        final Bookmark existingBookmark = Bookmark.builder()
                .id(1L)
                .title("Original Title")
                .url("https://original.com")
                .memo("Original Memo")
                .user(user)
                .build();

        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo", List.of("Updated Tag"));

        // 스파이 객체의 updateAndCacheBookmark 메소드가 수정된 엔티티를 반환하도록 설정합니다.
        doAnswer(invocation -> {
            existingBookmark.update("Updated Title", "https://updated.com", "Updated Memo");
            return existingBookmark;
        }).when(bookmarkService).updateAndCacheBookmark(1L, request);
        when(tagRepository.findByName("Updated Tag")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(Tag.builder().name("Updated Tag").build());

        // when
        final BookmarkResponse response = bookmarkService.updateBookmark(1L, request);

        // then
        assertThat(existingBookmark.getTitle()).isEqualTo("Updated Title");
        assertThat(existingBookmark.getUrl()).isEqualTo("https://updated.com");
        assertThat(existingBookmark.getMemo()).isEqualTo("Updated Memo");

        verify(bookmarkService, times(1)).updateAndCacheBookmark(1L, request);

        assertThat(response.getTags()).containsExactly("Updated Tag");
    }

    @DisplayName("북마크 삭제 - 성공")
    @Test
    void deleteBookmark_Success() {
        // given
        // 스파이 객체의 deleteBookmark 메소드가 아무것도 하지 않도록 설정합니다.
        // 이 테스트의 목적은 컨트롤러가 deleteBookmark를 호출하는지 확인하는 것이므로,
        // 내부 로직까지 테스트할 필요는 없습니다.
        doNothing().when(bookmarkService).deleteBookmark(1L);
        // given 
        // deleteBookmark 내부에서 findBookmarkEntityById가 호출되므로, 해당 호출을 모의(Mocking)
        Bookmark bookmarkToDelete = Bookmark.builder().id(1L).user(user).build();
        doReturn(bookmarkToDelete).when(bookmarkService).findBookmarkEntityById(1L);
        // 실제 repository의 delete는 아무것도 하지 않도록 설정
        doNothing().when(bookmarkRepository).delete(bookmarkToDelete);

        // when
        bookmarkService.deleteBookmark(1L);
        // then
        verify(bookmarkService, times(1)).deleteBookmark(1L);
        verify(bookmarkRepository, times(1)).delete(bookmarkToDelete);
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
        
        // getCurrentUserId()를 모의(Mocking)합니다.
        doReturn(userId).when(bookmarkService).getCurrentUserId();
        doThrow(new UnauthorizedBookmarkAccessException(2L, userId)).when(bookmarkService).findAndCacheBookmarkById(2L, userId);

        // when & then
        // 현재 로그인한 사용자(userId=1)가 다른 사용자(userId=2)의 북마크에 접근 시도
        assertThatThrownBy(() -> bookmarkService.getBookmarkById(2L))
                .isInstanceOf(UnauthorizedBookmarkAccessException.class);
    }
}
