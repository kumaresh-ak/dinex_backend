package com.example.dineX.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediaController {
    private final FileStorageService storage;

    public MediaController(FileStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/api/v1/public/media/{folder}/{name}")
    public ResponseEntity<Resource> publicMedia(@PathVariable String folder, @PathVariable String name) {
        if (!folder.matches("logos|qr|menu")) {
            return ResponseEntity.notFound().build();
        }
        return serve(folder + "/" + name);
    }

    @GetMapping("/api/v1/admin/media/proofs/{name}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Resource> proof(@PathVariable String name) {
        return serve("proofs/" + name);
    }

    private ResponseEntity<Resource> serve(String key) {
        Resource resource = storage.load(key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileStorageService.contentType(key)))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(key.substring(key.indexOf('/') + 1)).build().toString())
                .body(resource);
    }
}
