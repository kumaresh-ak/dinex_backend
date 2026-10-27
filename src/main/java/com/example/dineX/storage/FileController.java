package com.example.dineX.storage;

import com.example.dineX.common.ApiException;
import java.time.Duration;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Serves uploaded files. Stored keys look like {@code logos/<uuid>.png}. */
@RestController
public class FileController {
    private static final Set<String> PUBLIC_FOLDERS = Set.of("logos", "qr", "menu");

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    /** Logos, payment QR codes and menu images are public so guests and the storefront can render them. */
    @GetMapping("/api/v1/public/files/{folder}/{name:.+}")
    public ResponseEntity<Resource> publicFile(@PathVariable String folder, @PathVariable String name) {
        if (!PUBLIC_FOLDERS.contains(folder)) {
            throw ApiException.notFound("File");
        }
        return serve(folder + "/" + name, CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic());
    }

    /** Subscription payment proofs are restricted to SUPER_ADMIN by the /api/v1/admin/** security rule. */
    @GetMapping("/api/v1/admin/files/proofs/{name:.+}")
    public ResponseEntity<Resource> proof(@PathVariable String name) {
        return serve("proofs/" + name, CacheControl.noStore());
    }

    private ResponseEntity<Resource> serve(String key, CacheControl cache) {
        Resource resource = storage.load(key);
        return ResponseEntity.ok()
                .cacheControl(cache)
                .contentType(MediaType.parseMediaType(FileStorageService.contentType(key)))
                .body(resource);
    }
}
