package com.example.marker.dto;

import com.example.marker.domain.Bookmark;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BookmarkResponse {
    private final Long id;
    private final String title;
    private final String url;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private BookmarkResponse(Bookmark bookmark) {
        this.id = bookmark.getId();
        this.title = bookmark.getTitle();
        this.url = bookmark.getUrl();
        this.memo = bookmark.getMemo();
        this.createdAt = bookmark.getCreatedAt();
        this.updatedAt = bookmark.getUpdatedAt();
    }

    public static BookmarkResponse from(Bookmark bookmark) {
        return new BookmarkResponse(bookmark);
    }
}
