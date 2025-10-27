package com.example.marker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    private String memo;

    @Builder
    public Bookmark(String title, String url, String memo) {
        this.title = title;
        this.url = url;
        this.memo = memo;
    }

    public void update(String title, String url, String memo) {
        this.title = title;
        this.url = url;
        this.memo = memo;
    }
}
