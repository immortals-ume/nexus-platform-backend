package com.immortals.cache.features.compression;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GzipCompressionStrategyTest {

    @Test
    void testCompressData() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        String data = "This is a test string that should be compressed";
        byte[] original = data.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = strategy.compress(original);

        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
    }

    @Test
    void testCompressNull() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        byte[] compressed = strategy.compress(null);
        assertNull(compressed);
    }

    @Test
    void testCompressEmptyArray() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        byte[] compressed = strategy.compress(new byte[0]);
        assertNotNull(compressed);
        assertEquals(0, compressed.length);
    }

    @Test
    void testDecompressData() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        String data = "This is a test string that should be compressed";
        byte[] original = data.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = strategy.compress(original);
        byte[] decompressed = strategy.decompress(compressed);

        assertArrayEquals(original, decompressed);
    }

    @Test
    void testDecompressNull() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        byte[] decompressed = strategy.decompress(null);
        assertNull(decompressed);
    }

    @Test
    void testDecompressEmptyArray() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        byte[] decompressed = strategy.decompress(new byte[0]);
        assertNotNull(decompressed);
        assertEquals(0, decompressed.length);
    }

    @Test
    void testCompressDecompressRoundTrip() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        String data = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
        byte[] original = data.getBytes(StandardCharsets.UTF_8);

        byte[] compressed = strategy.compress(original);
        byte[] decompressed = strategy.decompress(compressed);

        assertArrayEquals(original, decompressed);
        String result = new String(decompressed, StandardCharsets.UTF_8);
        assertEquals(data, result);
    }

    @Test
    void testCompressionReducesSize() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is a repeating pattern. ");
        }
        byte[] original = sb.toString()
                .getBytes(StandardCharsets.UTF_8);

        byte[] compressed = strategy.compress(original);

        assertTrue(compressed.length < original.length,
                "Compressed size should be smaller than original for repetitive data");
    }

    @Test
    void testDecompressInvalidData() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        byte[] invalidData = "not gzip data".getBytes(StandardCharsets.UTF_8);

        assertThrows(CompressionException.class, () -> strategy.decompress(invalidData));
    }

    @Test
    void testGetAlgorithm() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        assertEquals("GZIP", strategy.getAlgorithm());
    }

    @Test
    void testCompressLargeData() {
        GzipCompressionStrategy strategy = new GzipCompressionStrategy();
        byte[] largeData = new byte[100000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        byte[] compressed = strategy.compress(largeData);
        byte[] decompressed = strategy.decompress(compressed);

        assertArrayEquals(largeData, decompressed);
    }
}
