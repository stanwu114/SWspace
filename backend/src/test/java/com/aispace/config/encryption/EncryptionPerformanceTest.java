package com.aispace.config.encryption;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 加密性能测试
 * 测试加密服务的性能表现
 */
@SpringBootTest
@ActiveProfiles("test")
class EncryptionPerformanceTest {

    @Autowired
    private EncryptionService encryptionService;

    @Test
    void encrypt_largeVolume_shouldMaintainPerformance() {
        // Given
        List<String> testData = generateTestData(1000, 100); // 1000条，每条100字符
        
        // When
        long startTime = System.currentTimeMillis();
        List<String> encryptedData = new ArrayList<>();
        
        for (String data : testData) {
            encryptedData.add(encryptionService.encrypt(data));
        }
        
        long encryptTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(encryptedData).hasSize(1000);
        assertThat(encryptTime).isLessThan(5000); // 5秒内完成
        System.out.println("加密1000条数据耗时: " + encryptTime + "ms");
    }

    @Test
    void decrypt_largeVolume_shouldMaintainPerformance() {
        // Given
        List<String> testData = generateTestData(1000, 100);
        List<String> encryptedData = new ArrayList<>();
        
        for (String data : testData) {
            encryptedData.add(encryptionService.encrypt(data));
        }
        
        // When
        long startTime = System.currentTimeMillis();
        List<String> decryptedData = new ArrayList<>();
        
        for (String encrypted : encryptedData) {
            decryptedData.add(encryptionService.decrypt(encrypted));
        }
        
        long decryptTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(decryptedData).hasSize(1000);
        assertThat(decryptedData).isEqualTo(testData);
        assertThat(decryptTime).isLessThan(5000); // 5秒内完成
        System.out.println("解密1000条数据耗时: " + decryptTime + "ms");
    }

    @Test
    void encrypt_smallVolume_shouldBeFast() {
        // Given
        List<String> testData = generateTestData(100, 50); // 100条，每条50字符
        
        // When
        long startTime = System.nanoTime();
        List<String> encryptedData = new ArrayList<>();
        
        for (String data : testData) {
            encryptedData.add(encryptionService.encrypt(data));
        }
        
        long totalTime = System.nanoTime() - startTime;
        double avgTimePerOperation = totalTime / 100.0 / 1_000_000.0; // 转换为毫秒
        
        // Then
        assertThat(avgTimePerOperation).isLessThan(10.0); // 平均每次操作小于10毫秒
        System.out.println("平均每次加密耗时: " + String.format("%.3f", avgTimePerOperation) + "ms");
    }

    @Test
    void decrypt_smallVolume_shouldBeFast() {
        // Given
        List<String> testData = generateTestData(100, 50);
        List<String> encryptedData = new ArrayList<>();
        
        for (String data : testData) {
            encryptedData.add(encryptionService.encrypt(data));
        }
        
        // When
        long startTime = System.nanoTime();
        List<String> decryptedData = new ArrayList<>();
        
        for (String encrypted : encryptedData) {
            decryptedData.add(encryptionService.decrypt(encrypted));
        }
        
        long totalTime = System.nanoTime() - startTime;
        double avgTimePerOperation = totalTime / 100.0 / 1_000_000.0; // 转换为毫秒
        
        // Then
        assertThat(avgTimePerOperation).isLessThan(10.0); // 平均每次操作小于10毫秒
        System.out.println("平均每次解密耗时: " + String.format("%.3f", avgTimePerOperation) + "ms");
    }

    @Test
    void isEncrypted_performance_shouldBeFast() {
        // Given
        String encrypted = encryptionService.encrypt("测试数据");
        String plain = "普通文本";
        
        // When - 测试大量调用
        long startTime = System.nanoTime();
        int encryptedCount = 0;
        int plainCount = 0;
        
        for (int i = 0; i < 10000; i++) {
            if (encryptionService.isEncrypted(encrypted)) {
                encryptedCount++;
            }
            if (encryptionService.isEncrypted(plain)) {
                plainCount++;
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        double avgTimePerOperation = totalTime / 20000.0 / 1_000_000.0; // 20000次调用
        
        // Then
        assertThat(encryptedCount).isEqualTo(10000);
        assertThat(plainCount).isEqualTo(0);
        assertThat(avgTimePerOperation).isLessThan(0.1); // 平均每次调用小于0.1毫秒
        System.out.println("isEncrypted平均耗时: " + String.format("%.6f", avgTimePerOperation) + "ms");
    }

    @Test
    void concurrent_encryption_shouldWork() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 100;
        List<Thread> threads = new ArrayList<>();
        List<List<String>> results = new ArrayList<>();
        
        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            List<String> threadResults = new ArrayList<>();
            results.add(threadResults);
            
            Thread thread = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String data = "线程" + threadIndex + "-数据" + j;
                    String encrypted = encryptionService.encrypt(data);
                    String decrypted = encryptionService.decrypt(encrypted);
                    threadResults.add(decrypted);
                }
            });
            
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(results).hasSize(threadCount);
        for (int i = 0; i < threadCount; i++) {
            assertThat(results.get(i)).hasSize(operationsPerThread);
            for (int j = 0; j < operationsPerThread; j++) {
                assertThat(results.get(i).get(j)).isEqualTo("线程" + i + "-数据" + j);
            }
        }
        
        System.out.println("并发加密测试完成，" + threadCount + "个线程，每个线程" + 
                          operationsPerThread + "次操作，总耗时: " + totalTime + "ms");
    }

    @Test
    void memory_usage_shouldBeReasonable() {
        // Given
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // When - 执行大量加密操作
        List<String> encryptedData = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            encryptedData.add(encryptionService.encrypt("内存测试数据" + i));
        }
        
        long afterEncryptionMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // 清理并运行垃圾回收
        encryptedData.clear();
        System.gc();
        Thread.yield();
        
        long afterGCMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Then
        long encryptionMemoryUsed = afterEncryptionMemory - initialMemory;
        long gcRecoveredMemory = afterEncryptionMemory - afterGCMemory;
        
        System.out.println("加密过程内存使用: " + (encryptionMemoryUsed / 1024 / 1024) + " MB");
        System.out.println("GC回收内存: " + (gcRecoveredMemory / 1024 / 1024) + " MB");
        
        // 内存增长应该在合理范围内
        assertThat(encryptionMemoryUsed).isLessThan(100 * 1024 * 1024); // 小于100MB
    }

    // 辅助方法：生成测试数据
    private List<String> generateTestData(int count, int length) {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; j++) {
                sb.append((char) ('A' + (i + j) % 26)); // 生成可预测的测试数据
            }
            data.add(sb.toString());
        }
        return data;
    }
}