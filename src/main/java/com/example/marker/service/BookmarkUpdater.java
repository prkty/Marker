package com.example.marker.service;

import com.example.marker.constants.CacheConstants;
import com.example.marker.domain.Bookmark;
import com.example.marker.dto.BookmarkUpdateRequest;
import org.springframework.cache.annotation.CacheEvict;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 북마크 수정 관련 로직을 담당하는 서비스입니다.
 * 북마크의 수정 및 캐시 갱신을 처리하여 단일 책임 원칙(SRP)을 따릅니다.
 * 캐싱(AOP)이 적용된 수정 메서드를 포함합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional // 이 서비스는 트랜잭션이 필요한 수정 작업을 처리합니다.
public class BookmarkUpdater {

    private final BookmarkFinder bookmarkFinder; // 북마크 조회를 위해 BookmarkFinder 주입
    private final BookmarkTagService bookmarkTagService; // 태그 관리를 위해 BookmarkTagService 주입

    /**
     * 북마크 정보를 수정하고, 그 결과를 캐시에 갱신합니다.
     * @CachePut이 올바르게 동작하도록 Bookmark 엔티티를 반환합니다.
     * @param userId 현재 사용자 ID
     * @param bookmarkId 수정할 북마크 ID
     * @param request 수정할 정보
     * @return 갱신된 Bookmark 엔티티
     */
    @CachePut(value = CacheConstants.BOOKMARK, key = "#userId + ':' + #bookmarkId")
    public Bookmark updateBookmarkAndCache(Long userId, Long bookmarkId, BookmarkUpdateRequest request) {
        // BookmarkFinder를 통해 북마크를 조회 (캐시 활용)
        Bookmark bookmark = bookmarkFinder.findBookmarkById(bookmarkId, userId);

        bookmark.update(request.getTitle(), request.getUrl(), request.getMemo());
        // BookmarkTagService를 통해 태그 업데이트
        bookmarkTagService.updateTagsForBookmark(bookmark, request.getTags());

        // @Transactional에 의해 영속성 컨텍스트의 변경 사항이 자동으로 DB에 반영됩니다.
        // bookmarkRepository.save(bookmark); // 명시적인 save는 필요 없을 수 있습니다.

        return bookmark;
    }

    /**
     * 북마크를 삭제하고, 캐시에서 해당 항목을 제거합니다.
     * @param userId 현재 사용자 ID
     * @param bookmarkId 삭제할 북마크 ID
     * @return 삭제될 Bookmark 엔티티
     */
    @CacheEvict(value = CacheConstants.BOOKMARK, key = "#userId + ':' + #bookmarkId")
    public Bookmark deleteBookmarkAndEvictCache(Long userId, Long bookmarkId) {
        // 삭제할 북마크를 먼저 조회하여 소유권을 확인합니다.
        Bookmark bookmarkToDelete = bookmarkFinder.findBookmarkById(bookmarkId, userId);
        return bookmarkToDelete;
    }
}