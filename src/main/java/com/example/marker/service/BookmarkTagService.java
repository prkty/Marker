package com.example.marker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.BookmarkTag;
import com.example.marker.domain.Tag;
import com.example.marker.repository.TagRepository;

import lombok.RequiredArgsConstructor;

/**
 * 북마크와 태그 간의 관계를 관리하는 서비스입니다.
 * 태그 생성, 북마크에 태그 연결/해제 등의 로직을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkTagService {

    private final TagRepository tagRepository;

    /**
     * 북마크의 태그 정보를 수정합니다. 기존의 모든 태그 연결을 지우고 새로운 태그 목록으로 교체합니다.
     * 이 메서드는 주로 북마크 수정 시 사용됩니다.
     * @param bookmark 태그를 수정할 북마크 엔티티
     * @param tagNames 새로운 태그 이름 목록
     */
    public void updateTagsForBookmark(Bookmark bookmark, List<String> tagNames) {
        // 기존 태그 연결 모두 삭제
        // orphanRemoval=true 옵션에 의해 BookmarkTag 엔티티가 DB에서 삭제됨
        bookmark.getBookmarkTags().clear();

        // 새로운 태그 연결
        associateTagsWithBookmark(bookmark, tagNames);
    }

    /**
     * 태그 이름 목록을 기반으로 북마크와 태그를 연결합니다.
     * 이 메서드는 북마크 생성 및 수정 시 새로운 태그를 연결하는 데 사용됩니다.
     * @param bookmark 태그를 연결할 북마크 엔티티
     * @param tagNames 태그 이름 목록
     */
    public void associateTagsWithBookmark(Bookmark bookmark, List<String> tagNames) {
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
}