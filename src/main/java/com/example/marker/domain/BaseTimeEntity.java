package com.example.marker.domain;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티가 공통으로 사용할 생성 및 수정 시간 필드를 정의한 추상 클래스입니다.
 * JPA Auditing 기능을 사용하여 엔티티가 생성되거나 수정될 때 시간을 자동으로 기록합니다.
 */
@Getter
@MappedSuperclass // 이 클래스를 상속받는 엔티티들은 아래 필드들을 컬럼으로 인식하게 됩니다.
@EntityListeners(AuditingEntityListener.class) // JPA에게 Auditing 기능을 사용한다고 알립니다.
public abstract class BaseTimeEntity {

    /**
     * 엔티티가 처음 생성될 때의 시간.
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 엔티티가 마지막으로 수정된 시간.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}