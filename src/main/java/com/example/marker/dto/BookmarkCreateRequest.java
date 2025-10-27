package com.example.marker.dto;

import com.example.marker.domain.Bookmark;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

/**
 * 북마크 생성을 위한 데이터 전송 객체(DTO)입니다.
 * Controller에서 @RequestBody를 통해 클라이언트의 JSON 요청을 이 객체로 변환합니다.
 */
@Getter
@Setter
@AllArgsConstructor
public class BookmarkCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "URL은 필수입니다.")
    @URL(message = "유효하지 않은 URL 형식입니다.")
    private String url;

    private String memo;

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
