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
        Bookmark bookmark = new Bookmark();
        bookmark.setTitle(this.title);
        bookmark.setUrl(this.url);
        bookmark.setMemo(this.memo);
        return bookmark;
    }
}
