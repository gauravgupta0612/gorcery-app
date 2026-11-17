package com.golagrocery.files;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService storage;

    @GetMapping("/files/catalog/{name:.+}")
    public ResponseEntity<Resource> catalog(@PathVariable String name) {
        return serve(storage.resolve("catalog", name));
    }

    @GetMapping("/files/uploads/**")
    public ResponseEntity<Resource> uploads(HttpServletRequest req) {
        String rel = req.getRequestURI().substring(req.getRequestURI().indexOf("/files/uploads/") + "/files/uploads/".length());
        return serve(storage.resolve("uploads", rel));
    }

    private ResponseEntity<Resource> serve(Path p) {
        Resource r = new FileSystemResource(p);
        MediaType type = MediaTypeFactory.getMediaType(r).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic()).body(r);
    }
}
