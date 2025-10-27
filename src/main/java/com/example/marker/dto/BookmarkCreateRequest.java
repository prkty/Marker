package com.example.marker.dto;

import com.example.marker.domain.Bookmark;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookmarkCreateRequest {
    private String title;
    private String url;
    private String memo;

    public Bookmark toEntity() {
        return Bookmark.builder()
                .title(this.title)
                .url(this.url)
                .memo(this.memo)
                .build();
    }
}
