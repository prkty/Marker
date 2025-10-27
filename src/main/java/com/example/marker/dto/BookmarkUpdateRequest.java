package com.example.marker.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookmarkUpdateRequest {
    private String title;
    private String url;
    private String memo;
}
