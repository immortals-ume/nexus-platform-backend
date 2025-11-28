package com.immortals.cache.features.compression;

/**
 * Strategy interface for compression algorithms.
 * Implementations provide specific compression/decompression logic.
 */
public interface CompressionStrategy {

    /**
     * Compresses the given data.
     *
     * @param data the data to compress
     * @return the compressed data
     * @throws CompressionException if compression fails
     */
    byte[] compress(byte[] data);

    /**
     * Decompresses the given data.
     *
     * @param data the data to decompress
     * @return the decompressed data
     * @throws CompressionException if decompression fails
     */
    byte[] decompress(byte[] data);

    /**
     * Returns the name of the compression algorithm.
     *
     * @return the algorithm name (e.g., "GZIP", "LZ4")
     */
    String getAlgorithm();
}
