package com.example.marker.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.marker.domain.Bookmark;
import com.example.marker.domain.BookmarkTag;
import com.example.marker.domain.Tag;
import com.example.marker.domain.User;

/**
 * BookmarkRepository에 대한 통합 테스트 클래스.
 * @DataJpaTest를 사용하여 JPA 관련 컴포넌트만 로드하고, 내장 데이터베이스를 사용합니다.
 * 각 테스트는 트랜잭션 내에서 실행되고 끝난 후 롤백됩니다.
 */
@DataJpaTest
class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BookmarkTagRepository bookmarkTagRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("test@example.com")
                .password("password")
                .build());
    }

    @DisplayName("북마크 저장 및 ID로 조회 테스트")
    @Test
    void saveAndFindById() {
        // given
        Bookmark bookmark = Bookmark.builder()
                .title("Google")
                .url("https://www.google.com")
                .memo("Search Engine")
                .user(user)
                .build();

        // when
        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        Optional<Bookmark> foundBookmark = bookmarkRepository.findById(savedBookmark.getId());

        // then
        assertThat(foundBookmark).isPresent();
        assertThat(foundBookmark.get().getId()).isEqualTo(savedBookmark.getId());
        assertThat(foundBookmark.get().getTitle()).isEqualTo("Google");
        assertThat(foundBookmark.get().getUrl()).isEqualTo("https://www.google.com");
    }

    @DisplayName("북마크 전체 조회 테스트")
    @Test
    void findAll() {
        // given
        Bookmark bookmark1 = Bookmark.builder().title("Google").url("https://www.google.com").user(user).build();
        Bookmark bookmark2 = Bookmark.builder().title("Naver").url("https://www.naver.com").user(user).build();
        bookmarkRepository.saveAll(List.of(bookmark1, bookmark2));

        // when
        Page<Bookmark> bookmarksPage = bookmarkRepository.findAll(PageRequest.of(0, 5));

        // then
        assertThat(bookmarksPage.getTotalElements()).isEqualTo(2);
        assertThat(bookmarksPage.getContent()).extracting("title", "url")
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("Google", "https://www.google.com"),
                        org.assertj.core.api.Assertions.tuple("Naver", "https://www.naver.com")
                );
    }

    @DisplayName("북마크 수정 테스트 (JPA 변경 감지)")
    @Test
    void update() {
        // given
        Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder().title("Original").url("https://original.com").user(user).build());

        // when
        Bookmark bookmarkToUpdate = bookmarkRepository.findById(savedBookmark.getId()).get();
        bookmarkToUpdate.update("Updated", "https://updated.com", "Updated Memo");
        bookmarkRepository.flush(); // 변경 감지(Dirty Checking)를 통한 업데이트 쿼리 실행

        // then
        Bookmark updatedBookmark = bookmarkRepository.findById(savedBookmark.getId()).get();
        assertThat(updatedBookmark.getTitle()).isEqualTo("Updated");
        assertThat(updatedBookmark.getUrl()).isEqualTo("https://updated.com");
        assertThat(updatedBookmark.getMemo()).isEqualTo("Updated Memo");
    }

    @DisplayName("북마크 삭제 테스트")
    @Test
    void delete() {
        // given
        Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder().title("To be deleted").url("https://delete.me").user(user).build());

        // when
        bookmarkRepository.deleteById(savedBookmark.getId());

        // then
        assertThat(bookmarkRepository.findById(savedBookmark.getId())).isEmpty();
    }

    @DisplayName("태그 이름으로 북마크 조회 테스트")
    @Test
    void findByTagName_Success() {
        // given
        // 1. 태그 저장
        Tag devTag = tagRepository.save(Tag.builder().name("개발").build());
        Tag newsTag = tagRepository.save(Tag.builder().name("뉴스").build());

        // 2. 북마크 저장
        Bookmark bookmark1 = bookmarkRepository.save(Bookmark.builder().title("Spring Blog").url("https://spring.io/blog").user(user).build());
        Bookmark bookmark2 = bookmarkRepository.save(Bookmark.builder().title("Naver News").url("https://news.naver.com").user(user).build());
        Bookmark bookmark3 = bookmarkRepository.save(Bookmark.builder().title("JPA Docs").url("https://docs.jboss.org/hibernate/orm/6.2/userguide/html_single/Hibernate_User_Guide.html").user(user).build());

        // 3. 북마크와 태그 연결
        bookmarkTagRepository.save(BookmarkTag.builder().bookmark(bookmark1).tag(devTag).build());
        bookmarkTagRepository.save(BookmarkTag.builder().bookmark(bookmark2).tag(newsTag).build());
        bookmarkTagRepository.save(BookmarkTag.builder().bookmark(bookmark3).tag(devTag).build());

        // when
        Page<Bookmark> devBookmarks = bookmarkRepository.findByUserIdAndTagName(user.getId(), "개발", PageRequest.of(0, 5));
        Page<Bookmark> newsBookmarks = bookmarkRepository.findByUserIdAndTagName(user.getId(), "뉴스", PageRequest.of(0, 5));
        Page<Bookmark> emptyBookmarks = bookmarkRepository.findByUserIdAndTagName(user.getId(), "쇼핑", PageRequest.of(0, 5));

        // then
        assertThat(devBookmarks.getTotalElements()).isEqualTo(2);
        assertThat(devBookmarks.getContent()).extracting("title").containsExactlyInAnyOrder("Spring Blog", "JPA Docs");

        assertThat(newsBookmarks.getTotalElements()).isEqualTo(1);
        assertThat(newsBookmarks.getContent().get(0).getTitle()).isEqualTo("Naver News");

        assertThat(emptyBookmarks.getTotalElements()).isEqualTo(0);
    }

    @DisplayName("키워드로 제목 또는 URL 검색 테스트 (findByUserIdAndKeyword)")
    @Test
    void findByUserIdAndKeyword_Success() {
        // given
        bookmarkRepository.save(Bookmark.builder().title("Spring Boot Guide").url("https://spring.io/guides").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("Naver News").url("https://news.naver.com").user(user).build());
        bookmarkRepository.save(Bookmark.builder().title("Google Search").url("https://www.google.com").user(user).build());

        // when
        Page<Bookmark> springResult = bookmarkRepository.findByUserIdAndKeyword(user.getId(), "spring", PageRequest.of(0, 5));
        Page<Bookmark> comResult = bookmarkRepository.findByUserIdAndKeyword(user.getId(), "com", PageRequest.of(0, 5));
        Page<Bookmark> emptyResult = bookmarkRepository.findByUserIdAndKeyword(user.getId(), "youtube", PageRequest.of(0, 5));

        // then
        assertThat(springResult.getTotalElements()).isEqualTo(1);
        assertThat(springResult.getContent().get(0).getTitle()).isEqualTo("Spring Boot Guide");

        assertThat(comResult.getTotalElements()).isEqualTo(2);
        assertThat(comResult.getContent()).extracting("title").containsExactlyInAnyOrder("Naver News", "Google Search");

        assertThat(emptyResult.getTotalElements()).isEqualTo(0);
    }
}