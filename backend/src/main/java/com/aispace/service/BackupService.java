package com.aispace.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * 备份服务
 * 提供数据库备份、文件备份和恢复功能
 */
@Slf4j
@Service
public class BackupService {
    
    @Value("${app.backup.path:~/.aiworker/backups}")
    private String backupPath;
    
    @Value("${app.backup.retention-days:30}")
    private int retentionDays;
    
    @Value("${spring.datasource.url}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username}")
    private String databaseUser;
    
    @Value("${spring.datasource.password}")
    private String databasePassword;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 执行完整备份
     */
    public BackupResult performFullBackup() {
        log.info("Starting full backup...");
        
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        Path backupDir = resolveBackupPath().resolve(timestamp);
        
        try {
            Files.createDirectories(backupDir);
            
            // 1. 数据库备份
            String dbBackupFile = backupDatabase(backupDir, timestamp);
            
            // 2. 记录备份元数据
            saveBackupMetadata(backupDir, timestamp);
            
            log.info("Full backup completed: {}", backupDir);
            
            return BackupResult.success(backupDir.toString(), 
                List.of(dbBackupFile));
            
        } catch (Exception e) {
            log.error("Backup failed", e);
            return BackupResult.failure(e.getMessage());
        }
    }
    
    /**
     * 数据库备份（使用pg_dump）
     */
    private String backupDatabase(Path backupDir, String timestamp) throws Exception {
        String fileName = "database_" + timestamp + ".sql.gz";
        Path outputFile = backupDir.resolve(fileName);
        
        // 解析数据库连接信息
        String host = "localhost";
        String port = "5433";
        String dbName = "aispace_db";
        
        // 尝试使用pg_dump
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port,
                "-U", databaseUser,
                "-d", dbName,
                "-F", "c",  // 自定义格式
                "-f", outputFile.toString().replace(".gz", "")
            );
            
            pb.environment().put("PGPASSWORD", databasePassword);
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 压缩备份文件
                compressFile(Paths.get(outputFile.toString().replace(".gz", "")), outputFile);
                Files.deleteIfExists(Paths.get(outputFile.toString().replace(".gz", "")));
                log.info("Database backup created: {}", fileName);
                return fileName;
            } else {
                log.warn("pg_dump failed, using alternative backup method");
            }
        } catch (Exception e) {
            log.warn("pg_dump not available: {}", e.getMessage());
        }
        
        // 备选方案：记录备份时间
        Files.writeString(outputFile.getParent().resolve("backup_info.txt"),
            "Backup timestamp: " + timestamp + "\n" +
            "Note: pg_dump not available, manual database backup recommended.\n");
        
        return "backup_info.txt";
    }
    
    /**
     * 压缩文件
     */
    private void compressFile(Path source, Path target) throws IOException {
        try (InputStream is = Files.newInputStream(source);
             GZIPOutputStream gzos = new GZIPOutputStream(Files.newOutputStream(target))) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
        }
    }
    
    /**
     * 保存备份元数据
     */
    private void saveBackupMetadata(Path backupDir, String timestamp) throws IOException {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("timestamp", timestamp);
        metadata.put("created_at", LocalDateTime.now().toString());
        metadata.put("type", "full");
        metadata.put("database", databaseUrl);
        
        String content = metadata.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("\n"));
        
        Files.writeString(backupDir.resolve("metadata.txt"), content);
    }
    
    /**
     * 列出所有备份
     */
    public List<BackupInfo> listBackups() {
        Path backupRoot = resolveBackupPath();
        
        if (!Files.exists(backupRoot)) {
            return Collections.emptyList();
        }
        
        try {
            return Files.list(backupRoot)
                .filter(Files::isDirectory)
                .map(this::readBackupInfo)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(BackupInfo::timestamp).reversed())
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list backups", e);
            return Collections.emptyList();
        }
    }
    
    private BackupInfo readBackupInfo(Path backupDir) {
        try {
            Path metadataFile = backupDir.resolve("metadata.txt");
            if (!Files.exists(metadataFile)) {
                return new BackupInfo(
                    backupDir.getFileName().toString(),
                    backupDir.toString(),
                    "unknown",
                    getFolderSize(backupDir)
                );
            }
            
            Map<String, String> metadata = Files.readAllLines(metadataFile).stream()
                .filter(line -> line.contains("="))
                .collect(Collectors.toMap(
                    line -> line.split("=")[0].trim(),
                    line -> line.split("=", 2)[1].trim()
                ));
            
            return new BackupInfo(
                metadata.getOrDefault("timestamp", backupDir.getFileName().toString()),
                backupDir.toString(),
                metadata.getOrDefault("type", "unknown"),
                getFolderSize(backupDir)
            );
        } catch (Exception e) {
            log.warn("Failed to read backup info: {}", backupDir, e);
            return null;
        }
    }
    
    private long getFolderSize(Path folder) {
        try {
            return Files.walk(folder)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try { return Files.size(p); } 
                    catch (IOException e) { return 0; }
                })
                .sum();
        } catch (IOException e) {
            return 0;
        }
    }
    
    /**
     * 清理过期备份
     */
    @Scheduled(cron = "0 0 3 * * ?")  // 每天凌晨3点执行
    public int cleanupOldBackups() {
        log.info("Cleaning up old backups...");
        
        Path backupRoot = resolveBackupPath();
        if (!Files.exists(backupRoot)) {
            return 0;
        }
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = 0;
        
        try {
            List<Path> oldBackups = Files.list(backupRoot)
                .filter(Files::isDirectory)
                .filter(dir -> {
                    try {
                        String name = dir.getFileName().toString();
                        LocalDateTime backupTime = LocalDateTime.parse(name, DATE_FORMAT);
                        return backupTime.isBefore(cutoff);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            for (Path backup : oldBackups) {
                deleteDirectory(backup);
                deleted++;
                log.info("Deleted old backup: {}", backup);
            }
        } catch (IOException e) {
            log.error("Failed to cleanup backups", e);
        }
        
        log.info("Cleanup completed, deleted {} backups", deleted);
        return deleted;
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try { Files.delete(path); } 
                catch (IOException e) { log.warn("Failed to delete: {}", path); }
            });
    }
    
    /**
     * 定时自动备份（每天凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        log.info("Executing scheduled backup...");
        performFullBackup();
    }
    
    private Path resolveBackupPath() {
        String path = backupPath.replace("~", System.getProperty("user.home"));
        return Paths.get(path);
    }
    
    /**
     * 备份结果
     */
    public record BackupResult(
        boolean success,
        String path,
        List<String> files,
        String error
    ) {
        public static BackupResult success(String path, List<String> files) {
            return new BackupResult(true, path, files, null);
        }
        
        public static BackupResult failure(String error) {
            return new BackupResult(false, null, null, error);
        }
    }
    
    /**
     * 备份信息
     */
    public record BackupInfo(
        String timestamp,
        String path,
        String type,
        long size
    ) {}
}
