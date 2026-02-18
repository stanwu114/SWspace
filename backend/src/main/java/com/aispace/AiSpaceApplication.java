package com.aispace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI员工协作系统 - 后端服务启动类
 * 
 * 面向政企智慧城市行业的"超级个人 + AI员工团队"协作系统
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class AiSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSpaceApplication.class, args);
        System.out.println("""
            
            ╔═══════════════════════════════════════════════════════════════╗
            ║                                                               ║
            ║     AI员工协作系统 - 后端服务启动成功                          ║
            ║                                                               ║
            ║     API文档: http://localhost:8080/swagger-ui.html            ║
            ║     健康检查: http://localhost:8080/actuator/health           ║
            ║                                                               ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }
}
