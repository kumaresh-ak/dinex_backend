package com.example.dineX.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.dineX.common.ApiException;
import com.example.dineX.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service() {
        AppProperties properties = mock(AppProperties.class);
        when(properties.storageDir()).thenReturn(tempDir.toString());
        return new FileStorageService(properties);
    }

    @Test
    void storesPngWithGeneratedName() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", png);

        String key = service().store(file, "menu");

        assertTrue(key.matches("menu/[a-f0-9-]{36}\\.png"));
        assertTrue(Files.isRegularFile(tempDir.resolve(key)));
    }

    @Test
    void storesWebpByRiffSignature() {
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", webp);

        String key = service().store(file, "qr");

        assertTrue(key.endsWith(".webp"));
    }

    @Test
    void rejectsExecutableContent() {
        byte[] exe = "MZ\u0090\u0000\u0003\u0000\u0000\u0000".getBytes(StandardCharsets.ISO_8859_1);
        MockMultipartFile file = new MockMultipartFile("file", "virus.png", "image/png", exe);

        assertThrows(ApiException.class, () -> service().store(file, "menu"));
    }

    @Test
    void rejectsUnknownFolder() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", png);

        assertThrows(ApiException.class, () -> service().store(file, "../secrets"));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThrows(ApiException.class, () -> service().store(file, "menu"));
    }

    @Test
    void loadRejectsPathTraversal() {
        assertThrows(ApiException.class, () -> service().load("menu/../../application.yml"));
    }
}
