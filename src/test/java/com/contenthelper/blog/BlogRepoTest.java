package com.contenthelper.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlogRepoTest {

  private final BlogRepo repo = new BlogRepo();

  private Path blogRepo(Path base) throws Exception {
    Files.createDirectories(base.resolve("src/content/blog"));
    return base;
  }

  @Test
  void rejectsNonRepoLayout(@TempDir Path base) {
    assertThat(repo.looksLikeBlogRepo(base)).isFalse();
  }

  @Test
  void writePendingLandsUnderPending(@TempDir Path base) throws Exception {
    Path repoPath = blogRepo(base);
    Path written = repo.writePending(repoPath, "hello", "body");
    assertThat(written).isEqualTo(repoPath.resolve("src/content/_pending/hello.md"));
    assertThat(Files.readString(written)).isEqualTo("body");
    assertThat(repo.pending(repoPath)).containsExactly("hello");
  }

  @Test
  void writePendingRefusesWhenAlreadyPublished(@TempDir Path base) throws Exception {
    Path repoPath = blogRepo(base);
    Files.writeString(repoPath.resolve("src/content/blog/hello.md"), "live");
    assertThatThrownBy(() -> repo.writePending(repoPath, "hello", "body"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Already published");
  }

  @Test
  void approveMovesPendingToContent(@TempDir Path base) throws Exception {
    Path repoPath = blogRepo(base);
    repo.writePending(repoPath, "hello", "body");
    Path moved = repo.approve(repoPath, "hello");
    assertThat(moved).isEqualTo(repoPath.resolve("src/content/blog/hello.md"));
    assertThat(repo.pending(repoPath)).isEmpty();
  }

  @Test
  void approveRefusesWhenNothingPending(@TempDir Path base) throws Exception {
    Path repoPath = blogRepo(base);
    assertThatThrownBy(() -> repo.approve(repoPath, "ghost"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Nothing pending");
  }
}
