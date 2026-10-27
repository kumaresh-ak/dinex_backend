package com.example.dineX.storage;

import com.example.dineX.common.ApiException;
import com.example.dineX.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    public static final Set<String> FOLDERS = Set.of("logos", "qr", "menu", "proofs");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path root;

    public FileStorageService(AppProperties properties) {
        this.root = Paths.get(properties.storageDir()).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file, String folder) {
        if (!FOLDERS.contains(folder)) {
            throw ApiException.badRequest("Unsupported upload folder");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File must be 5 MB or smaller");
        }
        String extension = detectExtension(file);
        if (extension == null) {
            throw ApiException.badRequest("Only JPG, PNG, WEBP images or PDF documents are allowed");
        }
        String name = UUID.randomUUID() + "." + extension;
        try {
            Path directory = root.resolve(folder);
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, directory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FAILED", "Unable to store the file");
        }
        return folder + "/" + name;
    }

    public Resource load(String key) {
        if (key == null || !key.matches("^(logos|qr|menu|proofs)/[a-f0-9-]{36}\\.(jpg|png|webp|pdf)$")) {
            throw ApiException.notFound("File");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw ApiException.notFound("File");
        }
        return new FileSystemResource(path);
    }

    public static String contentType(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "image/jpeg";
    }

    private String detectExtension(MultipartFile file) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(head, 0, head.length);
            if (read < 4) return null;
        } catch (IOException ex) {
            return null;
        }
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) return "jpg";
        if (startsWith(head, 0x89, 0x50, 0x4E, 0x47)) return "png";
        if (startsWith(head, 0x52, 0x49, 0x46, 0x46) && Arrays.equals(Arrays.copyOfRange(head, 8, 12),
                "WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) return "webp";
        if (startsWith(head, 0x25, 0x50, 0x44, 0x46)) return "pdf";
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) return false;
        }
        return true;
    }
}
