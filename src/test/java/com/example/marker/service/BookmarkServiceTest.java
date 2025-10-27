package com.example.marker.service;

import com.example.marker.domain.Bookmark;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

/**
 * BookmarkService에 대한 단위 테스트 클래스.
 * Mockito를 사용하여 Repository의 의존성을 격리하고 서비스 로직만 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    /**
     * @InjectMocks: 테스트 대상이 되는 클래스입니다.
     * Mockito가 @Mock으로 생성된 가짜 객체들을 이 클래스에 주입합니다.
     * 여기서는 BookmarkService의 인스턴스를 생성하고, bookmarkRepository Mock을 주입합니다.
     */
    @InjectMocks
    private BookmarkService bookmarkService;

    /**
     * @Mock: 가짜(Mock) 객체를 생성합니다.
     * BookmarkService는 BookmarkRepository에 의존하므로, 실제 데이터베이스와 상호작용하지 않도록
     * 가짜 Repository를 만들어 사용합니다.
     */
    @Mock
    private BookmarkRepository bookmarkRepository;


    @DisplayName("북마크 생성 - 성공")
    @Test
    void createBookmark_Success() {
        // given
        final BookmarkCreateRequest request = new BookmarkCreateRequest("Google", "https://www.google.com", "Search Engine");
        final Bookmark bookmark = Bookmark.builder()
                .id(1L)
                .title(request.getTitle())
                .url(request.getUrl())
                .memo(request.getMemo())
                .build();

        // repository.save()가 호출될 때의 가짜 동작 정의
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(bookmark);

        // when
        final BookmarkResponse response = bookmarkService.createBookmark(request);

        // then
        // 반환된 응답 값이 예상과 일치하는지 검증
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Google");
        assertThat(response.getUrl()).isEqualTo("https://www.google.com");
        assertThat(response.getMemo()).isEqualTo("Search Engine");

        // repository.save()가 한 번만 호출되었는지 검증
        verify(bookmarkRepository, times(1)).save(any(Bookmark.class));
    }

    @DisplayName("북마크 전체 조회 - 성공")
    @Test
    void getAllBookmarks_Success() {
        // given
        final Bookmark bookmark1 = Bookmark.builder().id(1L).title("Google").url("https://www.google.com").build();
        final Bookmark bookmark2 = Bookmark.builder().id(2L).title("Naver").url("https://www.naver.com").build();
        final List<Bookmark> bookmarks = List.of(bookmark1, bookmark2);

        when(bookmarkRepository.findAll()).thenReturn(bookmarks);

        // when
        final List<BookmarkResponse> responses = bookmarkService.getAllBookmarks();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting("title", "url")
                .containsExactlyInAnyOrder(
                        tuple("Google", "https://www.google.com"),
                        tuple("Naver", "https://www.naver.com")
                );

        verify(bookmarkRepository, times(1)).findAll();
    }

    @DisplayName("북마크 상세 조회 - 성공")
    @Test
    void getBookmarkById_Success() {
        // given
        final Bookmark bookmark = Bookmark.builder().id(1L).title("Google").url("https://www.google.com").memo("memo").build();
        when(bookmarkRepository.findById(1L)).thenReturn(Optional.of(bookmark));

        // when
        final BookmarkResponse response = bookmarkService.getBookmarkById(1L);

        // then
        assertThat(response.getTitle()).isEqualTo("Google");
        assertThat(response.getUrl()).isEqualTo("https://www.google.com");
        assertThat(response.getMemo()).isEqualTo("memo");

        verify(bookmarkRepository, times(1)).findById(1L);
    }

    @DisplayName("북마크 상세 조회 - 실패 (존재하지 않는 ID)")
    @Test
    void getBookmarkById_Fail_NotFound() {
        // given
        when(bookmarkRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.getBookmarkById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bookmark not found with id: 99");

        verify(bookmarkRepository, times(1)).findById(99L);
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
                .build();

        final BookmarkUpdateRequest request = new BookmarkUpdateRequest("Updated Title", "https://updated.com", "Updated Memo");

        when(bookmarkRepository.findById(1L)).thenReturn(Optional.of(existingBookmark));

        // when
        bookmarkService.updateBookmark(1L, request);

        // then
        assertThat(existingBookmark.getTitle()).isEqualTo("Updated Title");
        assertThat(existingBookmark.getUrl()).isEqualTo("https://updated.com");
        assertThat(existingBookmark.getMemo()).isEqualTo("Updated Memo");

        verify(bookmarkRepository, times(1)).findById(1L);
    }

    @DisplayName("북마크 삭제 - 성공")
    @Test
    void deleteBookmark_Success() {
        // given
        // void 메서드는 doNothing()으로 Mocking
        doNothing().when(bookmarkRepository).deleteById(1L);

        // when
        bookmarkService.deleteBookmark(1L);

        // then
        verify(bookmarkRepository, times(1)).deleteById(1L);
    }
}
