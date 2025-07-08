package com.github.clashautochange;

import com.github.clashautochange.service.AdminUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClashAutoChangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClashAutoChangeApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminUser(AdminUserService adminUserService) {
        return args -> {
            // 初始化默认管理员账号
            adminUserService.initDefaultAdmin();
        };
    }
}
