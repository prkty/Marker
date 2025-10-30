package com.example.marker.service;

import com.example.marker.constants.CacheConstants;
import com.example.marker.domain.Bookmark;
import com.example.marker.domain.BookmarkTag;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 북마크 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 트랜잭션 관리의 단위가 되며, Controller와 Repository 사이의 중재자 역할을 합니다.
 */
@Service
@Transactional(readOnly = true) // 클래스 전체에 읽기 전용 트랜잭션을 기본으로 설정
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository; // UserRepository 주입
    private final BookmarkService self; // 자기 자신을 주입받아 프록시를 통해 캐시 메소드를 호출

    public BookmarkService(BookmarkRepository bookmarkRepository, TagRepository tagRepository, UserRepository userRepository, @Lazy BookmarkService self) {
        this.bookmarkRepository = bookmarkRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.self = self;
    }

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

        // 태그 처리 로직 추가
        associateTagsWithBookmark(bookmark, request.getTags());

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
        Bookmark bookmark = self.findAndCacheBookmarkById(currentUserId, bookmarkId);
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
        Bookmark updatedBookmark = self.updateAndCacheBookmark(currentUserId, bookmarkId, request);
        return BookmarkResponse.from(updatedBookmark);
}

    /**
     * 북마크 정보를 수정하고, 그 결과를 캐시에 갱신하는 public 메소드.
     * @CachePut이 올바르게 동작하도록 Bookmark 엔티티를 반환합니다.
     * @param bookmarkId 수정할 북마크 ID
     * @param request 수정할 정보
     * @return 갱신된 Bookmark 엔티티
     */
    @CachePut(value = "bookmark", key = "#userId + ':' + #bookmarkId")
    public Bookmark updateAndCacheBookmark(Long userId, Long bookmarkId, BookmarkUpdateRequest request) {
        Bookmark bookmark = findBookmarkEntityById(userId, bookmarkId);
        bookmark.update(request.getTitle(), request.getUrl(), request.getMemo());
        updateTagsForBookmark(bookmark, request.getTags());
        return bookmark;
}

    /**
     * 특정 북마크를 삭제합니다.
     * @param bookmarkId 삭제할 북마크의 ID
     */
    @CacheEvict(value = "bookmark", key = "#userId + ':' + #bookmarkId")
@Transactional
public void deleteBookmark(Long bookmarkId) {
    Long userId = getCurrentUserId();
    Bookmark bookmarkToDelete = findBookmarkEntityById(userId, bookmarkId);
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


    /**
     * 북마크의 태그 정보를 수정합니다. 기존의 모든 태그 연결을 지우고 새로운 태그 목록으로 교체합니다.
     * @param bookmark 태그를 수정할 북마크 엔티티
     * @param tagNames 새로운 태그 이름 목록
     */
    private void updateTagsForBookmark(Bookmark bookmark, List<String> tagNames) {
        // 기존 태그 연결 모두 삭제
        // orphanRemoval=true 옵션에 의해 BookmarkTag 엔티티가 DB에서 삭제됨
        bookmark.getBookmarkTags().clear();

        // 새로운 태그 연결
        associateTagsWithBookmark(bookmark, tagNames);
    }

    /**
     * 태그 이름 목록을 기반으로 북마크와 태그를 연결합니다.
     * @param bookmark 태그를 연결할 북마크 엔티티
     * @param tagNames 태그 이름 목록
     */
    private void associateTagsWithBookmark(Bookmark bookmark, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        tagNames.forEach(tagName -> {
            // DB에서 태그 이름으로 태그를 찾거나, 없으면 새로 생성하여 저장
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));

            // Bookmark와 Tag를 연결하는 BookmarkTag 엔티티 생성
            BookmarkTag bookmarkTag = BookmarkTag.builder()
                    .bookmark(bookmark)
                    .tag(tag)
                    .build();
            bookmark.addBookmarkTag(bookmarkTag); // 북마크에 연결 정보 추가
        });
    }

    /**
     * ID로 북마크를 조회하고 결과를 캐시에 저장하는 public 메소드.
     * 이 메소드는 캐싱을 위해 격리된 DB 조회 지점 역할을 합니다.
     * @param bookmarkId 북마크 ID
     * @return 조회된 Bookmark 엔티티
     */
    @Cacheable(value = "bookmark", key = "#userId + ':' + #bookmarkId")
    public Bookmark findAndCacheBookmarkById(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findByIdWithTags(bookmarkId)
            .orElseThrow(() -> new BookmarkNotFoundException(bookmarkId));

    if (!bookmark.getUser().getId().equals(userId)) {
        throw new UnauthorizedBookmarkAccessException(bookmarkId, userId);
    }
    return bookmark;
}

    /**
     * ID로 북마크 엔티티를 찾는 중복 로직을 처리하는 public 메소드.
     * update, delete 등 내부 로직에서 재사용하기 위해 public으로 선언. (프록시 호출을 위해)
     * 이 메소드 자체는 캐싱되지 않음.
     * @param bookmarkId 북마크 ID
     * @return 조회된 Bookmark 엔티티
     */
    public Bookmark findBookmarkEntityById(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findByIdWithTags(bookmarkId)
            .orElseThrow(() -> new BookmarkNotFoundException(bookmarkId));

    if (!bookmark.getUser().getId().equals(userId)) {
        throw new UnauthorizedBookmarkAccessException(bookmarkId, userId);
    }
    return bookmark;
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