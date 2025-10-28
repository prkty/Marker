# 북마크 관리 API 서버 (Marker)

이 프로젝트는 개인 북마크를 관리하기 위한 RESTful API 서버입니다. Spring Boot를 기반으로 구축되었으며, 북마크의 생성, 조회, 수정, 삭제(CRUD) 기능을 제공합니다.

## 1. 프로젝트 구조 및 패키지 구성

본 프로젝트는 계층형 아키텍처(Layered Architecture)를 따르며, 각 패키지는 역할에 따라 명확하게 분리되어 있습니다.

### 1.1 프로젝트 구조

빌드```
src/main/java/com/example/marker
├── controller  # API의 엔드포인트를 정의하고, HTTP 요청/응답을 처리
├── domain      # 데이터베이스 테이블과 매핑되는 핵심 비즈니스 모델 (Entity)
├── dto         # 계층 간 데이터 전송을 위한 객체 (Data Transfer Object)
├── exception   # 애플리케이션에서 발생하는 예외를 정의하고 처리
├── repository  # Spring Data JPA를 사용하여 데이터베이스와 상호작용
└── service     # 핵심 비즈니스 로직을 구현하고, 트랜잭션을 관리
```

### 1.2 패키지 역할

-   **`controller`**: 클라이언트의 HTTP 요청을 수신하는 API 엔드포인트 계층입니다.
    -   `@RestController`를 사용하여 각 클래스를 API 컨트롤러로 정의합니다.
    -   요청 데이터를 DTO로 변환하고, 유효성을 검증(`@Valid`)한 후 서비스 계층으로 전달합니다.
    -   서비스 계층의 처리 결과를 `ResponseEntity`로 감싸 클라이언트에게 반환합니다.

-   **`domain`**: 애플리케이션의 핵심 비즈니스 모델인 엔티티(Entity)가 위치하는 패키지입니다.
    -   `@Entity` 어노테이션을 사용하여 데이터베이스 테이블과 매핑됩니다.
    -   엔티티와 관련된 핵심 비즈니스 로직(예: `update` 메소드)을 포함할 수 있습니다.

-   **`dto`**: 계층 간 데이터 전송을 위한 객체(Data Transfer Object)를 정의합니다.
    -   API의 요청(`Request`) 및 응답(`Response`) 명세를 나타내며, 엔티티와 프레젠테이션 계층을 분리하는 역할을 합니다.
    -   이를 통해 엔티티의 내부 구조가 외부에 직접 노출되는 것을 방지하고, API 명세 변경에 유연하게 대처할 수 있습니다.

-   **`exception`**: 애플리케이션 전역에서 발생하는 예외를 처리하는 로직을 담습니다.
    -   `@RestControllerAdvice`를 사용한 `GlobalExceptionHandler`를 통해 예외 상황에 대한 일관된 응답 형식을 제공합니다.
    -   `BookmarkNotFoundException`과 같은 커스텀 예외 클래스를 정의합니다.

-   **`repository`**: 데이터베이스와의 통신을 담당하는 데이터 접근 계층입니다.
    -   Spring Data JPA의 `JpaRepository` 인터페이스를 상속받아 기본적인 CRUD 메소드를 자동으로 구현합니다.
    -   필요에 따라 쿼리 메소드나 `@Query`를 사용하여 커스텀 쿼리를 정의할 수 있습니다.

-   **`service`**: 핵심 비즈니스 로직을 수행하는 계층입니다.
    -   `@Transactional` 어노테이션을 통해 트랜잭션을 관리합니다.
    -   컨트롤러로부터 전달받은 데이터를 가공하고, 리포지토리를 통해 데이터베이스와 상호작용하여 비즈니스 요구사항을 처리합니다.

---

## 2. 빌드, 실행 및 테스트 방법

### 2.1 요구사항

- Java 17
- Gradle 8.x

### 2.2 빌드 및 실행

1.  **프로젝트 빌드**

    프로젝트 루트 디렉터리에서 아래 명령어를 실행하여 프로젝트를 빌드합니다.

    ```bash
    gradlew.bat build
    ```

2.  **애플리케이션 실행**

    빌드된 JAR 파일을 실행하여 서버를 구동합니다.

    ```bash
    java -jar build/libs/Marker-0.0.1-SNAPSHOT.jar
    ```

    서버는 기본적으로 `8080` 포트에서 실행됩니다.

### 2.3 테스트 실행

프로젝트에 포함된 모든 단위 테스트 및 통합 테스트를 실행합니다.

```bash
gradlew.bat test
```

테스트 실행 후, `build/reports/tests/test/index.html` 경로에서 상세한 테스트 결과 리포트를 확인할 수 있습니다.

---

## 3 API 명세 확인 방법

API 명세는 두 가지 방법으로 확인할 수 있습니다.

### 3.1 Swagger UI

애플리케이션 실행 후, 아래 URL로 접속하면 API 문서를 시각적으로 확인하고 직접 테스트해볼 수 있습니다.

- **Swagger UI URL**: `http://localhost:8080/swagger-ui.html`

### 3.2 Markdown 문서

프로젝트 루트 디렉터리의 `api-docs.md` 파일에 API 명세가 Markdown 형식으로 정리되어 있습니다.

---

## 4 주요 설계 이유

1.  **DTO(Data Transfer Object) 패턴 적용**

    `Request`/`Response` DTO를 사용하여 각 계층 간의 역할을 명확히 분리했습니다. 이를 통해 Controller는 HTTP 요청/응답 처리에만 집중하고, Service는 비즈니스 로직에, Entity는 데이터베이스 매핑에만 집중할 수 있습니다. 이는 각 계층의 독립성을 높여 유지보수성과 확장성을 향상시킵니다.

2.  **계층별 테스트 전략 수립**

    -   **Controller 통합 테스트 (`@SpringBootTest`)**: 실제 서버와 유사한 환경에서 API의 전체 흐름을 검증하여 기능의 안정성을 보장합니다.
    -   **Service 단위 테스트 (`@Mock`)**: Repository를 Mocking하여 외부 의존성을 제거하고, 순수한 비즈니스 로직의 정확성을 빠르고 독립적으로 검증합니다.
    -   **Repository 통합 테스트 (`@DataJpaTest`)**: JPA 관련 설정만 로드하여 엔티티와 데이터베이스 간의 상호작용을 가볍고 신뢰성 있게 테스트합니다.

3.  **Swagger(OpenAPI)를 통한 API 문서 자동화**

    `springdoc-openapi` 라이브러리를 도입하여 코드 변경 시 API 문서가 자동으로 업데이트되도록 구성했습니다. 이를 통해 API 명세와 실제 구현 간의 불일치를 방지하고, 개발자가 문서 관리에 들이는 수고를 줄여 생산성을 높였습니다.

## 5 개선할 점 또는 아쉬운 점

1.  **미흡한 예외 처리**

    현재 존재하지 않는 북마크 ID로 조회/수정/삭제를 시도할 경우, `IllegalArgumentException`이 발생하여 `500 Internal Server Error`가 반환됩니다. 이는 RESTful 원칙에 맞지 않으며, 사용자에게 혼란을 줄 수 있습니다. 향후 `GlobalExceptionHandler`를 도입하여, `404 Not Found`와 같이 명확한 상태 코드와 일관된 에러 메시지를 반환하도록 개선해야 합니다.

2.  **생성/수정 시간 자동화 부재**

    `Bookmark` 엔티티의 `createdAt`, `updatedAt` 필드가 현재는 자동으로 관리되지 않고 있습니다. JPA Auditing 기능을 적용하여, 엔티티가 생성되거나 수정될 때 해당 시간이 데이터베이스에 자동으로 기록되도록 리팩토링할 필요가 있습니다.

3.  **페이지네이션 미적용**

    북마크 전체 조회 API(`GET /bookmarks`)가 현재 모든 데이터를 한 번에 반환하고 있습니다. 데이터의 양이 많아질 경우 성능 저하 및 메모리 문제를 유발할 수 있습니다. `Pageable`을 사용하여 페이지네이션 기능을 구현하고, 클라이언트가 원하는 만큼의 데이터만 요청할 수 있도록 개선해야 합니다.
