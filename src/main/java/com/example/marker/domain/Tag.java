package com.example.marker.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 태그 정보를 담는 JPA 엔티티 클래스입니다.
 * 데이터베이스의 'tag' 테이블과 매핑됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tag") // 테이블 이름 명시
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // 태그 이름 (예: "개발", "뉴스", "쇼핑")

    // BookmarkTag와의 양방향 관계 설정
    // Tag가 삭제될 때 연결된 BookmarkTag도 함께 삭제되도록 cascade 설정
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Builder 사용 시 기본값 설정
    private List<BookmarkTag> bookmarkTags = new ArrayList<>();

    // 태그 이름 변경을 위한 메소드 (필요 시)
    public void updateName(String name) {
        this.name = name;
    }

    // 편의 메소드: BookmarkTag 추가
    public void addBookmarkTag(BookmarkTag bookmarkTag) {
        this.bookmarkTags.add(bookmarkTag);
        bookmarkTag.setTag(this);
    }
}

