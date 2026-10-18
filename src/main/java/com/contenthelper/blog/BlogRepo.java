package com.contenthelper.blog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Talks to the Astro blog repo on disk. Generation writes only into the
 * pending area, which the blog loader ignores. Publishing is a separate,
 * deliberate move into the content dir.
 */
@Service
public class BlogRepo {

    public boolean looksLikeBlogRepo(Path blog) {
        return blog != null && Files.isDirectory(blog.resolve("src/content"));
    }

    public Path pendingDir(Path blog) {
        return blog.resolve("src/content/_pending");
    }

    public Path contentDir(Path blog) {
        return blog.resolve("src/content/blog");
    }

    public Path pendingPath(Path blog, String slug) {
        return pendingDir(blog).resolve(slug + ".md");
    }

    public Path publishedPath(Path blog, String slug) {
        return contentDir(blog).resolve(slug + ".md");
    }

    public Path writePending(Path blog, String slug, String content) throws IOException {
        Path target = pendingPath(blog, slug);
        if (Files.exists(target)) {
            throw new IllegalStateException("Already pending: " + target);
        }
        if (Files.exists(publishedPath(blog, slug))) {
            throw new IllegalStateException("Already published: " + publishedPath(blog, slug));
        }
        Files.createDirectories(pendingDir(blog));
        return Files.writeString(target, content);
    }

    public List<String> pending(Path blog) throws IOException {
        Path dir = pendingDir(blog);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".md"))
                    .map(p -> p.getFileName().toString().replaceAll("\\.md$", ""))
                    .sorted()
                    .toList();
        }
    }

    public Path approve(Path blog, String slug) throws IOException {
        Path src = pendingPath(blog, slug);
        Path dest = publishedPath(blog, slug);
        if (!Files.exists(src)) {
            throw new IllegalStateException("Nothing pending for slug " + slug);
        }
        if (Files.exists(dest)) {
            throw new IllegalStateException("Already published: " + dest);
        }
        Files.createDirectories(contentDir(blog));
        return Files.move(src, dest);
    }
}
