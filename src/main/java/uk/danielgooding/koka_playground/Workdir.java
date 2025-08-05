package uk.danielgooding.koka_playground;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

@RequestScope
@Component
public class Workdir {
    private Path path;
    private int uniqueCounter = 0;

    @PostConstruct
    void init() throws IOException {
        path = Files.createTempDirectory("workdir");
    }

    Path freshPath(String prefix) throws IOException {
        return path.resolve(String.format("%s-%d", prefix, uniqueCounter++));
    }

    @PreDestroy
    void cleanup() throws IOException {
        // recursively delete all
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NonNull FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NonNull FileVisitResult postVisitDirectory(@NonNull Path dir, IOException e)
                    throws IOException {
                if (e != null) throw e;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
