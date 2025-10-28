package com.example.marker.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Bookmark와 Tag의 다대다 관계를 연결하는 엔티티입니다.
 * 데이터베이스의 'bookmark_tag' 테이블과 매핑됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bookmark_tag") // 테이블 이름 명시
public class BookmarkTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookmark_id", nullable = false)
    private Bookmark bookmark;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    public BookmarkTag(Bookmark bookmark, Tag tag) {
        this.bookmark = bookmark;
        this.tag = tag;
    }

    // 연관관계 편의 메소드 (양방향 관계 설정 시 사용)
    public void setBookmark(Bookmark bookmark) {
        if (this.bookmark != null) {
            this.bookmark.getBookmarkTags().remove(this);
        }
        this.bookmark = bookmark;
        if (bookmark != null && !bookmark.getBookmarkTags().contains(this)) {
            bookmark.getBookmarkTags().add(this);
        }
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    // equals와 hashCode는 복합키를 사용하지 않으므로 id만으로 비교
}

