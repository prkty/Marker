package com.example.marker.repository;

import com.example.marker.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    // 태그 이름으로 태그를 찾는 쿼리 메소드
    Optional<Tag> findByName(String name);
}