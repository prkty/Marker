package com.example.marker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Marker API", version = "v1.0", description = "북마크 관리 API 서버"),
        // 모든 API에 전역적으로 보안 요구사항 적용
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth", // 보안 스키마의 이름
        type = SecuritySchemeType.HTTP, // 보안 스키마 타입
        scheme = "bearer", // HTTP 인증 스키마
        bearerFormat = "JWT", // Bearer 토큰의 형식
        in = SecuritySchemeIn.HEADER, // 토큰이 전달되는 위치
        paramName = "Authorization" // HTTP 헤더 이름
)
public class OpenApiConfig {
}