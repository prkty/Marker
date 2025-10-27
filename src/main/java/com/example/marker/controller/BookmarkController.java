package com.example.marker.controller;

import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.service.BookmarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 북마크 관련 HTTP 요청을 처리하는 컨트롤러 클래스입니다.
 * '/bookmarks' 경로의 요청을 받아 적절한 서비스 로직을 호출합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * 새 북마크를 생성합니다. (POST /bookmarks)
     * 요청 본문(RequestBody)으로 북마크 생성 데이터를 받아 처리합니다.
     * 생성 성공 시, HTTP 201 Created 상태 코드와 함께 생성된 북마크 정보를 반환합니다.
     *
     * @param request 북마크 생성 정보 DTO
     * @return 생성된 북마크 정보와 Location 헤더를 포함하는 ResponseEntity
     */
    @PostMapping
    public ResponseEntity<BookmarkResponse> createBookmark(@Valid @RequestBody BookmarkCreateRequest request) {
        BookmarkResponse response = bookmarkService.createBookmark(request);
        return ResponseEntity.created(URI.create("/bookmarks/" + response.getId())).body(response);
    }

    /**
     * 모든 북마크 목록을 조회합니다. (GET /bookmarks)
     *
     * @return 북마크 목록과 HTTP 200 OK 상태 코드를 포함하는 ResponseEntity
     */
    @GetMapping
    public ResponseEntity<List<BookmarkResponse>> getAllBookmarks() {
        List<BookmarkResponse> responses = bookmarkService.getAllBookmarks();
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 ID의 북마크를 상세 조회합니다. (GET /bookmarks/{id})
     *
     * @param id 조회할 북마크의 ID
     * @return 조회된 북마크 정보와 HTTP 200 OK 상태 코드를 포함하는 ResponseEntity
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookmarkResponse> getBookmarkById(@PathVariable Long id) {
        BookmarkResponse response = bookmarkService.getBookmarkById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 ID의 북마크 정보를 수정합니다. (PUT /bookmarks/{id})
     *
     * @param id 수정할 북마크의 ID
     * @param request 수정할 북마크 정보 DTO
     * @return 수정된 북마크 정보와 HTTP 200 OK 상태 코드를 포함하는 ResponseEntity
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(@PathVariable Long id, @Valid @RequestBody BookmarkUpdateRequest request) {
        BookmarkResponse response = bookmarkService.updateBookmark(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 ID의 북마크를 삭제합니다. (DELETE /bookmarks/{id})
     *
     * @param id 삭제할 북마크의 ID
     * @return 내용 없이 HTTP 204 No Content 상태 코드를 포함하는 ResponseEntity
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long id) {
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.noContent().build();
    }
}
