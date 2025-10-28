package com.example.marker.dto;

import com.example.marker.domain.Bookmark;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * 북마크 생성을 위한 데이터 전송 객체(DTO)입니다.
 * Controller에서 @RequestBody를 통해 클라이언트의 JSON 요청을 이 객체로 변환합니다.
 */
@Getter
@Setter
@AllArgsConstructor
public class BookmarkCreateRequest {

    @Schema(description = "북마크 제목", example = "크래프톤")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "북마크 URL", example = "https://www.krafton.com/")
    @NotBlank(message = "URL은 필수입니다.")
    @URL(message = "유효하지 않은 URL 형식입니다.")
    private String url;

    @Schema(description = "북마크 메모", example = "한국 게임 사이트")
    private String memo;

    @Schema(description = "북마크에 추가할 태그 목록", example = "[\"검색엔진\", \"IT\"]")
    private List<String> tags;

    /**
     * DTO 객체를 Bookmark 엔티티 객체로 변환합니다.
     * @return Bookmark 엔티티
     */
    public Bookmark toEntity() {
        return Bookmark.builder()
                .title(this.title)
                .url(this.url)
                .memo(this.memo)
                .build();
    }
}
