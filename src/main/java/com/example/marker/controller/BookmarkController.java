package com.example.marker.controller;

import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.marker.dto.BookmarkCreateRequest;
import com.example.marker.dto.BookmarkResponse;
import com.example.marker.dto.BookmarkUpdateRequest;
import com.example.marker.service.BookmarkService;

import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Bookmark API", description = "북마크 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @Operation(summary = "북마크 생성", description = "새로운 북마크를 시스템에 등록합니다.", operationId = "bookmark-01")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "북마크 생성 성공", content = @Content(schema = @Schema(implementation = BookmarkResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content)
    })
    @PostMapping
    public ResponseEntity<BookmarkResponse> createBookmark(@Valid @RequestBody BookmarkCreateRequest request) {
        BookmarkResponse response = bookmarkService.createBookmark(request);
        return ResponseEntity.created(URI.create("/bookmarks/" + response.getId())).body(response);
    }

    @Operation(summary = "북마크 목록 조회",
            description = "북마크 목록을 조회합니다. 'tag' 또는 'keyword' 쿼리 파라미터를 사용하여 필터링할 수 있습니다.",
            operationId = "bookmark-02")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content)
    })
    @GetMapping
    public ResponseEntity<Page<BookmarkResponse>> getBookmarks(
            @Parameter(description = "조회할 태그 이름 (선택)") @RequestParam(name = "tag", required = false) String tagName,
            @Parameter(description = "검색할 키워드 (제목 또는 URL, 선택)") @RequestParam(name = "keyword", required = false) String keyword,
            @ParameterObject Pageable pageable
    ) {
        Page<BookmarkResponse> responses;
        if (tagName != null && !tagName.isBlank()) {
            responses = bookmarkService.getBookmarksByTag(tagName, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            responses = bookmarkService.searchBookmarks(keyword, pageable);
        } else {
            responses = bookmarkService.getAllBookmarks(pageable);
        }
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "북마크 상세 조회", description = "지정된 ID의 북마크를 상세 조회합니다.", operationId = "bookmark-03")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = BookmarkResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없는 북마크", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 북마크", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookmarkResponse> getBookmarkById(@PathVariable Long id) {
        BookmarkResponse response = bookmarkService.getBookmarkById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "북마크 수정", description = "지정된 ID의 북마크 정보를 수정합니다.", operationId = "bookmark-04")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = BookmarkResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없는 북마크", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 북마크", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(@PathVariable Long id, @Valid @RequestBody BookmarkUpdateRequest request) {
        BookmarkResponse response = bookmarkService.updateBookmark(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "북마크 삭제", description = "지정된 ID의 북마크를 삭제합니다.", operationId = "bookmark-05")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", content = @Content),
            @ApiResponse(responseCode = "403", description = "접근 권한이 없는 북마크", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 북마크", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Long id) {
        bookmarkService.deleteBookmark(id);
        return ResponseEntity.noContent().build();
    }
}
