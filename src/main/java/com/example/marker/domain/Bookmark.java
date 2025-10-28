package com.example.marker.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * 북마크 정보를 담는 JPA 엔티티 클래스입니다.
 * 데이터베이스의 'bookmark' 테이블과 매핑됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bookmark extends BaseTimeEntity {

    /**
     * 북마크의 고유 식별자(ID). 데이터베이스에서 자동으로 생성됩니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 북마크의 제목. 필수 항목입니다.
     */
    @Column(nullable = false)
    private String title;

    /**
     * 북마크의 URL. 필수 항목입니다.
     */
    @Column(nullable = false)
    private String url;

    /**
     * 북마크에 대한 간단한 메모.
     */
    private String memo;

    /**
     * 북마크의 정보를 수정합니다.
     * @param title 수정할 제목
     * @param url 수정할 URL
     * @param memo 수정할 메모
     */
    public void update(String title, String url, String memo) {
        this.title = title;
        this.url = url;
        this.memo = memo;
    }

    /**
     * 북마크와 태그 간의 연결을 관리하는 컬렉션입니다.
     * Bookmark가 삭제될 때 연결된 BookmarkTag도 함께 삭제되도록 cascade 설정
     */
    @OneToMany(mappedBy = "bookmark", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Builder 사용 시 기본값 설정
    private List<BookmarkTag> bookmarkTags = new ArrayList<>();

    /**
     * 북마크에 태그를 추가하는 편의 메소드.
     * 양방향 관계의 무결성을 유지합니다.
     * @param bookmarkTag 추가할 BookmarkTag 엔티티
     */
    public void addBookmarkTag(BookmarkTag bookmarkTag) {
        this.bookmarkTags.add(bookmarkTag);
        bookmarkTag.setBookmark(this);
    }
}