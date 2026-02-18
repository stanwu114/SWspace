package com.aispace.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 应用重启服务
 * 用于在配置更新后重启应用
 */
@Slf4j
@Service
public class RestartService {

    private final ApplicationContext applicationContext;

    public RestartService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 重启应用
     * 使用延迟执行确保HTTP响应能够返回给客户端
     */
    public void restart() {
        log.info("应用将在2秒后重启...");
        
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                // 获取启动类
                Class<?> mainClass = getMainClass();
                if (mainClass == null) {
                    log.error("无法找到主类，无法重启");
                    return;
                }
                
                // 关闭当前应用
                if (applicationContext instanceof ConfigurableApplicationContext ctx) {
                    ctx.close();
                }
                
                // 重新启动
                log.info("正在重启应用...");
                SpringApplication.run(mainClass);
                
            } catch (Exception e) {
                log.error("重启应用失败", e);
            }
        }, 2, TimeUnit.SECONDS);
    }

    /**
     * 获取主类
     * 通过堆栈跟踪找到包含 main 方法的类
     */
    private Class<?> getMainClass() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!className.startsWith("java.") && 
                    !className.startsWith("sun.") &&
                    !className.startsWith("org.springframework.") &&
                    !className.startsWith("com.aispace.service.")) {
                    try {
                        Class<?> clazz = Class.forName(className);
                        // 检查是否有 main 方法
                        try {
                            clazz.getMethod("main", String[].class);
                            return clazz;
                        } catch (NoSuchMethodException ignored) {
                            // 不是主类，继续查找
                        }
                    } catch (ClassNotFoundException ignored) {
                        // 类找不到，继续
                    }
                }
            }
        } catch (Exception e) {
            log.error("查找主类失败", e);
        }
        
        // 默认返回已知的主类
        try {
            return Class.forName("com.aispace.AiSpaceApplication");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
