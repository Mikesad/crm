package com.crm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能企业 CRM 系统启动类
 *
 * <p>技术栈：Spring Boot 3.x + JDK 17 + Sa-Token + MyBatis-Plus + MySQL 8.0</p>
 *
 * @author crm-team
 * @since 1.0.0
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.crm.mapper")
public class CrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
        System.out.println("""

                ====================================================
                  CRM 智能企业管理系统 启动成功
                  接口前缀: http://localhost:8080/api
                  接口文档: http://localhost:8080/api/doc.html
                ====================================================
                """);
    }
}
