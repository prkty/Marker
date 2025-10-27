package com.example.marker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

/**
 * 북마크 수정을 위한 데이터 전송 객체(DTO)입니다.
 * Controller에서 @RequestBody를 통해 클라이언트의 JSON 요청을 이 객체로 변환합니다.
 */
@Getter
@Setter
public class BookmarkUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "URL은 필수입니다.")
    @URL(message = "유효하지 않은 URL 형식입니다.")
    private String url;

    private String memo;
}
