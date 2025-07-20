package com.github.clashautochange;

import com.github.clashautochange.service.AdminUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class ClashAutoChangeApplication {

    public static void main(String[] args) {
        // 设置应用程序默认时区为中国时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
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
