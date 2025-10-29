package com.example.marker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableCaching // 캐싱 기능 활성화
@EnableJpaAuditing // JPA Auditing 기능 활성화
@SpringBootApplication
public class MarkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarkerApplication.class, args);
	}

}
