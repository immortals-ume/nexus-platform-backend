package com.immortals.cache.features.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP compression strategy implementation.
 * Provides standard GZIP compression and decompression.
 */
public class GzipCompressionStrategy implements CompressionStrategy {
    
    private static final int BUFFER_SIZE = 8192;
    
    @Override
    public byte[] compress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
             GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            
            gzipStream.write(data);
            gzipStream.finish();
            return byteStream.toByteArray();
            
        } catch (IOException e) {
            throw new CompressionException("Failed to compress data using GZIP", e);
        }
    }
    
    @Override
    public byte[] decompress(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
             GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = gzipStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            throw new CompressionException("Failed to decompress data using GZIP", e);
        }
    }
    
    @Override
    public String getAlgorithm() {
        return "GZIP";
    }
}
