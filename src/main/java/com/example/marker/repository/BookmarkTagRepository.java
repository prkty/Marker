package com.example.marker.repository;

import com.example.marker.domain.BookmarkTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkTagRepository extends JpaRepository<BookmarkTag, Long> {
}