package com.example.marker.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException; // Keep Bookmark import for return types
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.marker.constants.CacheConstants;
import com.example.marker.domain.Bookmark;
import com.example.marker.domain.User;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import com.example.marker.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 북마크 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 트랜잭션 관리의 단위가 되며, Controller와 Repository 사이의 중재자 역할을 합니다.
 */
@Service
@Transactional(readOnly = true) // 클래스 전체에 읽기 전용 트랜잭션을 기본으로 설정
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository; // UserRepository 주입
    private final BookmarkFinder bookmarkFinder; // 북마크 조회 로직 분리
    private final BookmarkUpdater bookmarkUpdater; // 북마크 수정 로직 분리
    private final BookmarkTagService bookmarkTagService; // 태그 관리 로직 분리

    /**
     * 새로운 북마크를 생성합니다.
     * @param request 북마크 생성에 필요한 데이터
     * @return 생성된 북마크 정보
     */
    @Transactional // 개별적으로 쓰기 트랜잭션을 적용
    public BookmarkResponse createBookmark(BookmarkCreateRequest request) {
        Long currentUserId = getCurrentUserId();
        // 현재 인증된 사용자 정보를 가져와 북마크에 연결
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found.")); // 이 예외는 토큰이 유효하면 발생하지 않아야 함
        Bookmark bookmark = request.toEntity(currentUser);

        // BookmarkTagService를 통해 태그 연결
        bookmarkTagService.associateTagsWithBookmark(bookmark, request.getTags());

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return BookmarkResponse.from(savedBookmark);
    }

    /**
     * 모든 북마크 목록을 조회합니다.
     * @return 북마크 목록
     */
    public Page<BookmarkResponse> getAllBookmarks(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        Page<Bookmark> bookmarks = bookmarkRepository.findAllByUserId(currentUserId, pageable);
        return bookmarks.map(BookmarkResponse::from);
    }

    /**
     * ID를 이용하여 특정 북마크를 조회합니다.
     * @param bookmarkId 조회할 북마크의 ID
     * @return 조회된 북마크 정보
     * @throws IllegalArgumentException 해당 ID의 북마크가 없을 경우
     */
    public BookmarkResponse getBookmarkById(Long bookmarkId) {
        Long currentUserId = getCurrentUserId();
    Bookmark bookmark = bookmarkFinder.findBookmarkById(bookmarkId, currentUserId); // BookmarkFinder 사용
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
        Long currentUserId = getCurrentUserId();
        Bookmark updatedBookmark = bookmarkUpdater.updateBookmarkAndCache(currentUserId, bookmarkId, request); // BookmarkUpdater 사용
        return BookmarkResponse.from(updatedBookmark);
}

    /**
     * 특정 북마크를 삭제합니다.
     * @param bookmarkId 삭제할 북마크의 ID
     */
    @Transactional
    public void deleteBookmark(Long bookmarkId, Long userId) {
        Long currentUserId = getCurrentUserId(); // 파라미터 대신 현재 인증된 사용자 ID를 사용
        Bookmark bookmarkToDelete = bookmarkUpdater.deleteBookmarkAndEvictCache(currentUserId, bookmarkId);
        bookmarkRepository.delete(bookmarkToDelete);
    }

    /**
     * 특정 태그를 가진 모든 북마크 목록을 조회합니다.
     * @param tagName 조회할 태그 이름
     * @return 해당 태그를 가진 북마크 목록
     */
    public Page<BookmarkResponse> getBookmarksByTag(String tagName, Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdAndTagName(currentUserId, tagName, pageable);
        return bookmarks.map(BookmarkResponse::from);
    }

    /**
     * 제목 또는 URL에 특정 키워드가 포함된 북마크 목록을 검색합니다.
     * @param keyword 검색할 키워드
     * @return 검색된 북마크 목록
     */
    public Page<BookmarkResponse> searchBookmarks(String keyword, Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdAndKeyword(currentUserId, keyword, pageable);
        return bookmarks.map(BookmarkResponse::from);
    }

    
    // 현재 로그인한 사용자의 ID를 가져오는 헬퍼 메소드
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User not authenticated."); // 인증되지 않은 사용자 접근 시
        }
        // 필터에서 principal로 사용자 ID(String)를 설정했으므로, getName()으로 바로 가져올 수 있음
        return Long.parseLong(authentication.getName());
    }
}