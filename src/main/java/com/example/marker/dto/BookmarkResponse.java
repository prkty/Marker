package com.example.marker.dto;

import com.example.marker.domain.Bookmark;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 북마크 정보 응답을 위한 데이터 전송 객체(DTO)입니다.
 * Service 계층에서 생성되어 Controller를 통해 클라이언트에게 JSON 형태로 반환됩니다.
 * 엔티티의 모든 정보를 노출하지 않고, 필요한 데이터만 가공하여 전달하는 역할을 합니다.
 */
@Getter
public class BookmarkResponse {
    private final Long id;
    private final String title;
    private final String url;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    /**
     * Bookmark 엔티티를 인자로 받아 DTO 객체를 생성하는 private 생성자입니다.
     * 정적 팩토리 메소드 'from'을 통해서만 객체를 생성할 수 있도록 제한합니다.
     * @param bookmark 응답을 생성할 Bookmark 엔티티
     */
    private BookmarkResponse(Bookmark bookmark) {
        this.id = bookmark.getId();
        this.title = bookmark.getTitle();
        this.url = bookmark.getUrl();
        this.memo = bookmark.getMemo();
        this.createdAt = bookmark.getCreatedAt();
        this.updatedAt = bookmark.getUpdatedAt();
    }

    /**
     * Bookmark 엔티티를 BookmarkResponse DTO로 변환하는 정적 팩토리 메소드입니다.
     * @param bookmark 변환할 Bookmark 엔티티
     * @return 생성된 BookmarkResponse 객체
     */
    public static BookmarkResponse from(Bookmark bookmark) {
        return new BookmarkResponse(bookmark);
    }
}