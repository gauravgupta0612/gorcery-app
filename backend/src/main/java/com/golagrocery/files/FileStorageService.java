package com.golagrocery.files;

import com.golagrocery.common.ApiException;
import com.golagrocery.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Stores uploaded images on local disk. Swap for S3/GCS by replacing this service in production. */
@Service
public class FileStorageService {
    private static final Map<String, String> ALLOWED = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
    private final Path root;
    private final Path seedRoot;

    public FileStorageService(AppProperties props) throws IOException {
        this.root = Paths.get(props.storage().dir()).toAbsolutePath().normalize();
        this.seedRoot = Paths.get(props.storage().seedDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose an image");
        String ext = ALLOWED.get(file.getContentType());
        if (ext == null) throw ApiException.badRequest("Only JPG, PNG or WEBP images are allowed");
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (!looksLikeImage(head)) throw ApiException.badRequest("File is not a valid image");
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
        LocalDate d = LocalDate.now();
        String rel = d.getYear() + "/" + String.format("%02d", d.getMonthValue()) + "/" + UUID.randomUUID() + "." + ext;
        Path target = root.resolve(rel).normalize();
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
        return "/files/uploads/" + rel;
    }

    static boolean looksLikeImage(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return true; // JPEG
        if (h.length >= 8 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return true;      // PNG
        return h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    }

    /** Resolves a public URL path to a file, preventing path traversal. */
    public Path resolve(String area, String relative) {
        Path base = "catalog".equals(area) ? seedRoot : root;
        Path p = base.resolve(relative).normalize();
        if (!p.startsWith(base) || !Files.isRegularFile(p)) throw ApiException.notFound("File");
        return p;
    }
}
