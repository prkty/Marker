package com.example.marker.service;

import com.example.marker.constants.CacheConstants;
import com.example.marker.domain.Bookmark;
import com.example.marker.exception.BookmarkNotFoundException;
import com.example.marker.exception.UnauthorizedBookmarkAccessException;
import com.example.marker.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 북마크 조회 관련 로직을 담당하는 서비스입니다.
 * 복잡한 비즈니스 로직과 조회 로직을 분리하여 단일 책임 원칙(SRP)을 따릅니다.
 * 캐싱(AOP)이 적용된 조회 메서드를 포함합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkFinder {

    private final BookmarkRepository bookmarkRepository;

    /**
     * ID로 북마크를 조회합니다.
     * 캐시 키에 사용자 ID를 포함하여 다른 사용자의 캐시와 충돌하지 않도록 합니다.
     *
     * @param bookmarkId 조회할 북마크 ID
     * @param userId     현재 사용자 ID
     * @return 조회된 Bookmark 엔티티
     * @throws BookmarkNotFoundException 북마크를 찾을 수 없는 경우
     */
    @Cacheable(value = CacheConstants.BOOKMARK, key = "#userId + ':' + #bookmarkId", unless = "#result == null")
    public Bookmark findBookmarkById(Long bookmarkId, Long userId) {
        Bookmark bookmark = bookmarkRepository.findByIdWithTags(bookmarkId) // 태그와 함께 북마크 조회
            .orElseThrow(() -> new BookmarkNotFoundException(bookmarkId));

        if (!bookmark.getUser().getId().equals(userId)) {
            throw new UnauthorizedBookmarkAccessException(bookmarkId, userId);
        }
        return bookmark;
    }
}
