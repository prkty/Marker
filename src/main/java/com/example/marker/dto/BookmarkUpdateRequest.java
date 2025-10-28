package com.example.marker.dto;

import org.hibernate.validator.constraints.URL;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 북마크 수정을 위한 데이터 전송 객체(DTO)입니다.
 * Controller에서 @RequestBody를 통해 클라이언트의 JSON 요청을 이 객체로 변환합니다.
 */
@Getter
@Setter
@AllArgsConstructor
public class BookmarkUpdateRequest {

    @Schema(description = "북마크 제목", example = "구글")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "북마크 URL", example = "https://www.google.com")
    @NotBlank(message = "URL은 필수입니다.")
    @URL(message = "유효하지 않은 URL 형식입니다.")
    private String url;

    @Schema(description = "북마크 메모", example = "세계 최대 검색 엔진")
    private String memo;
}
