package com.example.marker.service;

import com.example.marker.domain.Bookmark;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 북마크 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 트랜잭션 관리의 단위가 되며, Controller와 Repository 사이의 중재자 역할을 합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 전체에 읽기 전용 트랜잭션을 기본으로 설정
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    /**
     * 새로운 북마크를 생성합니다.
     * @param request 북마크 생성에 필요한 데이터
     * @return 생성된 북마크 정보
     */
    @Transactional // 개별적으로 쓰기 트랜잭션을 적용
    public BookmarkResponse createBookmark(BookmarkCreateRequest request) {
        Bookmark bookmark = request.toEntity();
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return BookmarkResponse.from(savedBookmark);
    }

    /**
     * 모든 북마크 목록을 조회합니다.
     * @return 북마크 목록
     */
    public List<BookmarkResponse> getAllBookmarks() {
        return bookmarkRepository.findAll().stream()
                .map(BookmarkResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ID를 이용하여 특정 북마크를 조회합니다.
     * @param bookmarkId 조회할 북마크의 ID
     * @return 조회된 북마크 정보
     * @throws IllegalArgumentException 해당 ID의 북마크가 없을 경우
     */
    public BookmarkResponse getBookmarkById(Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + bookmarkId));
        return BookmarkResponse.from(bookmark);
    }

    /**
     * 특정 북마크의 정보를 수정합니다.
     * @param bookmarkId 수정할 북마크의 ID
     * @param request 수정할 북마크 데이터
     * @return 수정된 북마크 정보
     * @throws IllegalArgumentException 해당 ID의 북마크가 없을 경우
     */
    @Transactional
    public BookmarkResponse updateBookmark(Long bookmarkId, BookmarkUpdateRequest request) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + bookmarkId));

        // 엔티티의 update 메소드를 호출하여 변경 감지(Dirty Checking) 기능을 활용
        bookmark.update(request.getTitle(), request.getUrl(), request.getMemo());

        return BookmarkResponse.from(bookmark);
    }

    /**
     * 특정 북마크를 삭제합니다.
     * @param bookmarkId 삭제할 북마크의 ID
     */
    @Transactional
    public void deleteBookmark(Long bookmarkId) {
        bookmarkRepository.deleteById(bookmarkId);
    }
}