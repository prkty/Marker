# Marker API 명세서

이 문서는 북마크 관리 API 서버의 명세를 정의합니다. 응답 예시를 포함했으나 반복적인 예시는 생략되었습니다.

**기본 URL**: `http://localhost:8080`

---
## 인증 (Authentication)

본 API의 `/auth` 경로를 제외한 모든 엔드포인트는 인증이 필요합니다.

인증을 위해서는 `POST /auth/login` API를 통해 발급받은 JWT(JSON Web Token)를 HTTP 요청 헤더에 포함해야 합니다.

- **Header**: `Authorization`
- **Value**: `Bearer <YOUR_JWT_TOKEN>`

---

## 1. 인증 API (Auth API)

### 1.1 회원가입

- **Endpoint**: `POST /auth/signup`
- **Description**: 이메일과 비밀번호로 새로운 사용자를 등록합니다.

#### 요청 (Request)
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123!"
  }
  ```

#### 응답 (Response)
- **✅ 201 Created**: 회원가입 성공.
- **❌ 400 Bad Request**: 요청 값 유효성 검증 실패 (이메일 형식, 비밀번호 길이 등).
```json
  {
    "timestamp": "2023-11-21T10:30:00",
    "status": 400,
    "error": "Bad Request",
    "message": "입력 값에 대한 유효성 검증에 실패했습니다.",
    "path": "/auth/signup",
    "errors": [
      {
        "field": "email",
        "value": "invalid-email",
        "reason": "유효한 이메일 형식이 아닙니다."
      },
      {
        "field": "password",
        "value": "123",
        "reason": "비밀번호는 최소 8자 이상이어야 합니다."
      }
    ]
  }
  ```

- **❌ 409 Conflict**: 이미 존재하는 이메일.
  ```json
  {
    "timestamp": "2023-11-21T10:30:00",
    "status": 409,
    "error": "Conflict",
    "message": "User with email test@example.com already exists.",
    "path": "/auth/signup"
  }
  ```

</br>

### 1.2 로그인

- **Endpoint**: `POST /auth/login`
- **Description**: 이메일과 비밀번호로 로그인하여 인증 토큰(JWT)을 발급받습니다.

#### 요청 (Request)
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "password123!"
  }
  ```

#### 응답 (Response)
- **✅ 200 OK**: 로그인 성공.
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
  ```
- **❌ 400 Bad Request**: 요청 값 유효성 검증 실패.
- **❌ 401 Unauthorized**: 이메일 또는 비밀번호가 일치하지 않음.
  ```json
  {
    "timestamp": "2023-11-21T10:30:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password.",
    "path": "/auth/login"
  }
  ```

</br>

### 1.3 로그아웃

- **Endpoint**: `POST /auth/logout`
- **Description**: 서버에 로그아웃을 알립니다. (JWT는 클라이언트 측에서 토큰을 삭제하여 로그아웃을 처리합니다.)

#### 요청 (Request)
- **Headers**: `Authorization: Bearer <token>`

#### 응답 (Response)
- **✅ 200 OK**: 로그아웃 요청 성공.

---

## 2. 북마크 API (Bookmark API)

**※ 모든 북마크 API는 `Authorization: Bearer <token>` 헤더가 필요합니다.**

### 2.1 북마크 생성

- **Endpoint**: `POST /bookmarks`
- **Description**: 현재 로그인한 사용자의 새로운 북마크를 생성합니다.

#### 요청 (Request)
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "title": "크래프톤",
    "url": "https://www.krafton.com/",
    "memo": "한국 대표 게임 사이트",
    "tags": ["게임", "IT"]
  }
  ```

#### 응답 (Response)
- **✅ 201 Created**: 생성 성공. `Location` 헤더에 생성된 리소스의 URI가 포함됩니다.
  ```json
  {
    "id": 1,
    "title": "크래프톤",
    "url": "https://www.krafton.com/",
    "memo": "한국 대표 게임 사이트",
    "tags": ["게임", "IT"],
    "createdAt": "2023-11-21T10:00:00",
    "updatedAt": "2023-11-21T10:00:00"
  }
  ```
- **❌ 400 Bad Request**: 요청 값 유효성 검증 실패.
- **❌ 401 Unauthorized / 403 Forbidden**: 인증되지 않은 사용자.

</br>

### 2.2 북마크 목록 조회

- **Endpoint**: `GET /bookmarks`
- **Description**: 현재 로그인한 사용자의 북마크 목록을 조회합니다.

#### 요청 (Request)
- **Query Parameters**:
  - `page` (optional, integer): 조회할 페이지 번호 (0부터 시작).
  - `size` (optional, integer): 한 페이지에 표시할 항목 수.
  - `sort` (optional, string): 정렬 기준 (예: `createdAt,desc`).
  - `tag` (optional, string): 특정 태그를 가진 북마크만 필터링.
  - `keyword` (optional, string): 제목 또는 URL에 포함된 키워드로 검색.

#### 응답 (Response)
- **✅ 200 OK**: 조회 성공. (페이지네이션 정보 포함)
  ```json
  {
    "content": [
      {
        "id": 1,
        "title": "크래프톤",
        "url": "https://www.krafton.com/",
        "memo": "한국 대표 게임 사이트",
        "tags": ["게임", "IT"],
        "createdAt": "2023-11-21T10:00:00",
        "updatedAt": "2023-11-21T10:00:00"
      }
    ],
    "pageable": { ... },
    "totalElements": 1,
    ...
  }
  ```
- **❌ 401 Unauthorized / 403 Forbidden**: 인증되지 않은 사용자.

</br>

### 2.3 북마크 상세 조회

- **Endpoint**: `GET /bookmarks/{id}`
- **Description**: 현재 로그인한 사용자의 특정 북마크를 상세 조회합니다.

#### 요청 (Request)
- **Path Parameters**:
  - `id` (required, long): 조회할 북마크의 ID.

#### 응답 (Response)
- **✅ 200 OK**: 조회 성공.
  ```json
  {
    "id": 1,
    "title": "크래프톤",
    "url": "https://www.krafton.com/",
    "memo": "한국 대표 게임 사이트",
    "tags": ["게임", "IT"],
    "createdAt": "2023-11-21T10:00:00",
    "updatedAt": "2023-11-21T10:00:00"
  }
  ```

- **❌ 401 Unauthorized / 403 Forbidden**: 인증되지 않았거나, 자신의 북마크가 아닐 경우. (예시는 403 기준)
  ```json
  {
    "timestamp": "2023-11-21T10:30:00",
    "status": 403,
    "error": "Forbidden",
    "message": "User with ID 2 is not authorized to access bookmark with ID 1",
    "path": "/bookmarks/1"
  }
  ```

- **❌ 404 Not Found**: 해당 ID의 북마크가 존재하지 않을 경우.
  ```json
  {
    "timestamp": "2023-11-21T10:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "Bookmark not found with id: 999",
    "path": "/bookmarks/999"
  }
  ```

</br>

### 2.4 북마크 수정

- **Endpoint**: `PUT /bookmarks/{id}`
- **Description**: 현재 로그인한 사용자의 특정 북마크를 수정합니다.

#### 요청 (Request)
- **Path Parameters**:
  - `id` (required, long): 수정할 북마크의 ID.
- **Content-Type**: `application/json`
- **Body**:
  ```json
  {
    "title": "크래프톤",
    "url": "https://www.krafton.com/",
    "memo": "수정된 메모",
    "tags": ["수정된태그"]
  }
  ```

#### 응답 (Response)
- **✅ 200 OK**: 수정 성공. 수정된 북마크 정보를 반환합니다.
  ```json
  {
    "id": 1,
    "title": "크래프톤",
    "url": "https://www.krafton.com/",
    "memo": "수정된 메모",
    "tags": ["수정된태그"],
    "createdAt": "2023-11-21T10:00:00",
    "updatedAt": "2023-11-21T10:45:00"
  }
  ```

- **❌ 400 Bad Request**: 요청 값 유효성 검증 실패.
- **❌ 401 Unauthorized / 403 Forbidden**: 인증되지 않았거나, 자신의 북마크가 아닐 경우.
- **❌ 404 Not Found**: 해당 ID의 북마크가 존재하지 않을 경우.
  ```json
  {
    "timestamp": "2023-11-21T10:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "Bookmark not found with id: 999",
    "path": "/bookmarks/999"
  }
  ```

</br>

### 2.5 북마크 삭제

- **Endpoint**: `DELETE /bookmarks/{id}`
- **Description**: 현재 로그인한 사용자의 특정 북마크를 삭제합니다.

#### 요청 (Request)
- **Path Parameters**:
  - `id` (required, long): 삭제할 북마크의 ID.

#### 응답 (Response)
- **✅ 204 No Content**: 삭제 성공.
- **❌ 401 Unauthorized / 403 Forbidden**: 인증되지 않았거나, 자신의 북마크가 아닐 경우.
- **❌ 404 Not Found**: 해당 ID의 북마크가 존재하지 않을 경우.
