package com.example.marker.service;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.BookmarkTag;
import com.example.marker.domain.Tag;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import com.example.marker.repository.TagRepository;
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
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 전체에 읽기 전용 트랜잭션을 기본으로 설정
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TagRepository tagRepository;

    /**
     * 새로운 북마크를 생성합니다.
     * @param request 북마크 생성에 필요한 데이터
     * @return 생성된 북마크 정보
     */
    @Transactional // 개별적으로 쓰기 트랜잭션을 적용
    public BookmarkResponse createBookmark(BookmarkCreateRequest request) {
        Bookmark bookmark = request.toEntity();

        // 태그 처리 로직 추가
        associateTagsWithBookmark(bookmark, request.getTags());

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

        // 1. 북마크 기본 정보 수정
        bookmark.update(request.getTitle(), request.getUrl(), request.getMemo());

        // 2. 태그 정보 수정 (기존 태그 모두 제거 후 새로 추가)
        updateTagsForBookmark(bookmark, request.getTags());

        return BookmarkResponse.from(bookmark);
    }

    /**
     * 특정 북마크를 삭제합니다.
     * @param bookmarkId 삭제할 북마크의 ID
     */
    @Transactional
    public void deleteBookmark(Long bookmarkId) {
        // 삭제하려는 북마크가 존재하는지 확인 (404 응답을 위해)
        if (!bookmarkRepository.existsById(bookmarkId)) {
            throw new IllegalArgumentException("Bookmark not found with id: " + bookmarkId);
        }
        bookmarkRepository.deleteById(bookmarkId);
    }

    /**
     * 특정 태그를 가진 모든 북마크 목록을 조회합니다.
     * @param tagName 조회할 태그 이름
     * @return 해당 태그를 가진 북마크 목록
     */
    public List<BookmarkResponse> getBookmarksByTag(String tagName) {
        List<Bookmark> bookmarks = bookmarkRepository.findByTagName(tagName);
        return bookmarks.stream()
                .map(BookmarkResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 제목 또는 URL에 특정 키워드가 포함된 북마크 목록을 검색합니다.
     * @param keyword 검색할 키워드
     * @return 검색된 북마크 목록
     */
    public List<BookmarkResponse> searchBookmarks(String keyword) {
        List<Bookmark> bookmarks = bookmarkRepository.findByTitleContainingIgnoreCaseOrUrlContainingIgnoreCase(keyword, keyword);
        return bookmarks.stream()
                .map(BookmarkResponse::from)
                .collect(Collectors.toList());
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
}