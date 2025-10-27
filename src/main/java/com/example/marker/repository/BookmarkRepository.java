package com.example.marker.repository;

import com.example.marker.domain.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 북마크 엔티티에 대한 데이터베이스 작업을 처리하는 Spring Data JPA 리포지토리입니다.
 * JpaRepository를 상속받아 기본적인 CRUD(Create, Read, Update, Delete) 메소드를 자동으로 제공받습니다.
 */
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
}