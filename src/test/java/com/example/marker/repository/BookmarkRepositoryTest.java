package com.example.marker.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.marker.domain.Bookmark;

/**
 * BookmarkRepository에 대한 통합 테스트 클래스.
 * @DataJpaTest를 사용하여 JPA 관련 컴포넌트만 로드하고, 내장 데이터베이스를 사용합니다.
 * 각 테스트는 트랜잭션 내에서 실행되고 끝난 후 롤백됩니다.
 */
@DataJpaTest
class BookmarkRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @DisplayName("북마크 저장 및 ID로 조회 테스트")
    @Test
    void saveAndFindById() {
        // given
        Bookmark bookmark = Bookmark.builder()
                .title("Google")
                .url("https://www.google.com")
                .memo("Search Engine")
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
        Bookmark bookmark1 = Bookmark.builder().title("Google").url("https://www.google.com").build();
        Bookmark bookmark2 = Bookmark.builder().title("Naver").url("https://www.naver.com").build();
        bookmarkRepository.saveAll(List.of(bookmark1, bookmark2));

        // when
        List<Bookmark> bookmarks = bookmarkRepository.findAll();

        // then
        assertThat(bookmarks).hasSize(2);
        assertThat(bookmarks).extracting("title", "url")
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("Google", "https://www.google.com"),
                        org.assertj.core.api.Assertions.tuple("Naver", "https://www.naver.com")
                );
    }

    @DisplayName("북마크 수정 테스트 (JPA 변경 감지)")
    @Test
    void update() {
        // given
        Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder().title("Original").url("https://original.com").build());

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
        Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder().title("To be deleted").url("https://delete.me").build());

        // when
        bookmarkRepository.deleteById(savedBookmark.getId());

        // then
        assertThat(bookmarkRepository.findById(savedBookmark.getId())).isEmpty();
    }
}