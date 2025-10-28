package com.example.marker.repository;

import com.example.marker.domain.Bookmark;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 북마크 엔티티에 대한 데이터베이스 작업을 처리하는 Spring Data JPA 리포지토리입니다.
 * JpaRepository를 상속받아 기본적인 CRUD(Create, Read, Update, Delete) 메소드를 자동으로 제공받습니다.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 특정 태그 이름을 포함하는 모든 북마크를 조회합니다.
     * @param tagName 조회할 태그의 이름
     * @return 해당 태그를 가진 북마크 목록
     */
    @Query("SELECT b FROM Bookmark b JOIN b.bookmarkTags bt JOIN bt.tag t WHERE t.name = :tagName")
    List<Bookmark> findByTagName(@Param("tagName") String tagName);

    /**
     * 제목 또는 URL에 특정 키워드가 포함된 모든 북마크를 대소문자 구분 없이 조회합니다.
     * @param titleKeyword 제목에서 검색할 키워드
     * @param urlKeyword URL에서 검색할 키워드
     * @return 해당 키워드를 포함하는 북마크 목록
     */
    List<Bookmark> findByTitleContainingIgnoreCaseOrUrlContainingIgnoreCase(String titleKeyword, String urlKeyword);
}