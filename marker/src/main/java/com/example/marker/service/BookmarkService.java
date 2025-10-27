package com.example.marker.service;

import com.example.marker.domain.Bookmark;
import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public BookmarkResponse createBookmark(BookmarkCreateRequest request) {
        Bookmark bookmark = request.toEntity();
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return BookmarkResponse.from(savedBookmark);
    }

    public List<BookmarkResponse> getAllBookmarks() {
        return bookmarkRepository.findAll().stream()
                .map(BookmarkResponse::from)
                .collect(Collectors.toList());
    }

    public BookmarkResponse getBookmarkById(Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + bookmarkId));
        return BookmarkResponse.from(bookmark);
    }

    @Transactional
    public BookmarkResponse updateBookmark(Long bookmarkId, BookmarkUpdateRequest request) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found with id: " + bookmarkId));

        bookmark.setTitle(request.getTitle());
        bookmark.setUrl(request.getUrl());
        bookmark.setMemo(request.getMemo());

        return BookmarkResponse.from(bookmark);
    }

    @Transactional
    public void deleteBookmark(Long bookmarkId) {
        bookmarkRepository.deleteById(bookmarkId);
    }
}
