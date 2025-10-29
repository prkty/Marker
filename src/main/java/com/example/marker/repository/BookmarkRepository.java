package com.example.marker.repository;

import com.example.marker.domain.Bookmark;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 북마크 엔티티에 대한 데이터베이스 작업을 처리하는 Spring Data JPA 리포지토리입니다.
 * JpaRepository를 상속받아 기본적인 CRUD(Create, Read, Update, Delete) 메소드를 자동으로 제공받습니다.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 특정 사용자의 모든 북마크를 페이징하여 조회합니다.
     * @param userId 사용자의 ID
     * @param pageable 페이징 정보
     * @return 해당 사용자의 북마크 페이지
     */
    Page<Bookmark> findAllByUserId(Long userId, Pageable pageable);
    /**
     * 특정 태그 이름을 포함하는 모든 북마크를 조회합니다.
     * @param userId 조회할 사용자의 ID
     * @param tagName 조회할 태그의 이름
     * @return 해당 태그를 가진 북마크 목록
     */
    @Query("SELECT b FROM Bookmark b JOIN b.bookmarkTags bt JOIN bt.tag t WHERE b.user.id = :userId AND t.name = :tagName")
    Page<Bookmark> findByUserIdAndTagName(@Param("userId") Long userId, @Param("tagName") String tagName, Pageable pageable);

    /**
     * 제목 또는 URL에 특정 키워드가 포함된 모든 북마크를 대소문자 구분 없이 조회합니다.
     * @param userId 조회할 사용자의 ID
     * @param titleKeyword 제목에서 검색할 키워드
     * @param urlKeyword URL에서 검색할 키워드
     * @return 해당 키워드를 포함하는 북마크 목록
     */
    @Query("SELECT b FROM Bookmark b WHERE b.user.id = :userId AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.url) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Bookmark> findByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * ID로 북마크를 조회할 때, 연관된 태그 정보까지 함께 가져옵니다. (N+1 문제 해결)
     * @param id 조회할 북마크의 ID
     * @return 태그 정보가 포함된 북마크 Optional 객체
     */
    @Query("SELECT b FROM Bookmark b LEFT JOIN FETCH b.user LEFT JOIN FETCH b.bookmarkTags bt LEFT JOIN FETCH bt.tag WHERE b.id = :id")
    Optional<Bookmark> findByIdWithTags(@Param("id") Long id);
}