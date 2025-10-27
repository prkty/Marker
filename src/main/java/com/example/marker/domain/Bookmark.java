package com.example.marker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 북마크 정보를 담는 JPA 엔티티 클래스입니다.
 * 데이터베이스의 'bookmark' 테이블과 매핑됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
     * 빌더 패턴을 사용한 생성자입니다.
     * @param title 북마크 제목
     * @param url 북마크 URL
     * @param memo 북마크 메모
     */
    @Builder
    public Bookmark(String title, String url, String memo) {
        this.title = title;
        this.url = url;
        this.memo = memo;
    }

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
}