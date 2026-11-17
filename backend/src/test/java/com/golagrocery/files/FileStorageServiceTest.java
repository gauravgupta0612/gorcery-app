package com.golagrocery.files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {
    @Test
    void detectsImageMagicBytes() {
        assertTrue(FileStorageService.looksLikeImage(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertTrue(FileStorageService.looksLikeImage(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}));
        assertFalse(FileStorageService.looksLikeImage("<?php echo 1;".getBytes()));
    }
}
