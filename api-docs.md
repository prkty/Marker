# Bookmark API 명세서

이 문서는 북마크 관리 API 서버의 명세를 정의합니다.

**기본 URL**: `http://localhost:8080`

---

## 1. 북마크 생성

새로운 북마크를 시스템에 추가합니다.

- **Endpoint**: `POST /bookmarks`
- **Description**: 새로운 북마크를 생성합니다.

### 요청 (Request)

#### Request Body

```json
{
  "title": "string (required)",
  "url": "string (required, URL format)",
  "memo": "string (optional)"
}
```

- **Content-Type**: `application/json`

#### 필드 설명

| 필드    | 타입   | 제약 조건                | 설명             |
|-------|------|------------------------|----------------|
| `title` | String | 필수, 공백 불가            | 북마크의 제목      |
| `url`   | String | 필수, 공백 불가, URL 형식 | 북마크의 URL 주소  |
| `memo`  | String | 선택                     | 북마크에 대한 메모 |

### 응답 (Response)

#### ✅ 성공: 201 Created

북마크 생성이 성공했을 때 반환됩니다. `Location` 헤더에 생성된 리소스의 URI가 포함됩니다.

**Headers**:
- `Location`: `/bookmarks/{created-id}`

**Body**:

```json
{
  "id": 1,
  "title": "Naver",
  "url": "https://www.naver.com",
  "memo": "한국 대표 포털 사이트",
  "createdAt": "2023-11-21T10:00:00",
  "updatedAt": "2023-11-21T10:00:00"
}
```

#### ❌ 실패: 400 Bad Request

요청 본문의 유효성 검증(Validation)에 실패했을 때 반환됩니다. (예: 필수 필드 누락, URL 형식 오류)

**Body**:

```json
{
    "timestamp": "2023-11-21T10:05:00.123Z",
    "status": 400,
    "error": "Bad Request",
    "errors": [
        {
            "field": "title",
            "defaultMessage": "제목은 필수입니다."
        }
    ],
    "path": "/bookmarks"
}
```

---

## 2. 북마크 전체 조회

시스템에 저장된 모든 북마크 목록을 조회합니다.

- **Endpoint**: `GET /bookmarks`
- **Description**: 모든 북마크 목록을 조회합니다.

### 요청 (Request)

요청 본문이나 파라미터가 필요 없습니다.

### 응답 (Response)

#### ✅ 성공: 200 OK

**Body**:

```json
[
  {
    "id": 1,
    "title": "Naver",
    "url": "https://www.naver.com",
    "memo": "한국 대표 포털 사이트",
    "createdAt": "2023-11-21T10:00:00",
    "updatedAt": "2023-11-21T10:00:00"
  },
  {
    "id": 2,
    "title": "Google",
    "url": "https://www.google.com",
    "memo": "세계 최대 검색 엔진",
    "createdAt": "2023-11-21T10:02:00",
    "updatedAt": "2023-11-21T10:02:00"
  }
]
```

---

## 3. 북마크 상세 조회

특정 ID를 가진 북마크의 상세 정보를 조회합니다.

- **Endpoint**: `GET /bookmarks/{id}`
- **Description**: 지정된 ID의 북마크를 상세 조회합니다.

### 요청 (Request)

#### Path Parameters

| 파라미터 | 타입 | 설명             |
|--------|------|----------------|
| `id`     | Long | 조회할 북마크의 ID |

### 응답 (Response)

#### ✅ 성공: 200 OK

**Body**:

```json
{
  "id": 1,
  "title": "Naver",
  "url": "https://www.naver.com",
  "memo": "한국 대표 포털 사이트",
  "createdAt": "2023-11-21T10:00:00",
  "updatedAt": "2023-11-21T10:00:00"
}
```

#### ❌ 실패: 404 Not Found

해당 ID의 북마크가 존재하지 않을 경우 반환됩니다. (※ 전역 예외 처리기 구현 시 응답 형식은 달라질 수 있습니다.)

---

## 4. 북마크 수정

특정 ID를 가진 북마크의 정보를 수정합니다.

- **Endpoint**: `PUT /bookmarks/{id}`
- **Description**: 지정된 ID의 북마크 정보를 수정합니다.

### 요청 (Request)

#### Path Parameters

| 파라미터 | 타입 | 설명             |
|--------|------|----------------|
| `id`     | Long | 수정할 북마크의 ID |

#### Request Body

```json
{
  "title": "string (required)",
  "url": "string (required, URL format)",
  "memo": "string (optional)"
}
```

- **Content-Type**: `application/json`

### 응답 (Response)

#### ✅ 성공: 200 OK

수정된 북마크의 정보를 반환합니다.

**Body**:

```json
{
  "id": 1,
  "title": "네이버",
  "url": "https://www.naver.com",
  "memo": "수정된 메모",
  "createdAt": "2023-11-21T10:00:00",
  "updatedAt": "2023-11-21T11:30:00"
}
```

#### ❌ 실패: 400 Bad Request / 404 Not Found

- `400 Bad Request`: 요청 본문의 유효성 검증에 실패한 경우.
- `404 Not Found`: 수정하려는 ID의 북마크가 존재하지 않는 경우.

---

## 5. 북마크 삭제

특정 ID를 가진 북마크를 삭제합니다.

- **Endpoint**: `DELETE /bookmarks/{id}`
- **Description**: 지정된 ID의 북마크를 삭제합니다.

### 요청 (Request)

#### Path Parameters

| 파라미터 | 타입 | 설명             |
|--------|------|----------------|
| `id`     | Long | 삭제할 북마크의 ID |

### 응답 (Response)

#### ✅ 성공: 204 No Content

삭제 성공 시, 응답 본문 없이 상태 코드만 반환됩니다.

#### ❌ 실패: 404 Not Found

삭제하려는 ID의 북마크가 존재하지 않을 경우 반환됩니다.

