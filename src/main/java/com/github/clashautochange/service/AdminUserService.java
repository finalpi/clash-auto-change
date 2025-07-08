package com.github.clashautochange.service;

import com.github.clashautochange.entity.AdminUser;
import com.github.clashautochange.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AdminUserService(AdminUserRepository adminUserRepository, BCryptPasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 初始化默认管理员账号
     */
    public void initDefaultAdmin() {
        if (adminUserRepository.count() == 0) {
            AdminUser adminUser = new AdminUser();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("123456"));
            adminUserRepository.save(adminUser);
        }
    }

    /**
     * 验证用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否验证成功
     */
    public boolean authenticate(String username, String password) {
        Optional<AdminUser> userOptional = adminUserRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            AdminUser user = userOptional.get();
            return passwordEncoder.matches(password, user.getPassword());
        }
        return false;
    }

    /**
     * 获取管理员用户
     *
     * @param username 用户名
     * @return 管理员用户
     */
    public Optional<AdminUser> getAdminUser(String username) {
        return adminUserRepository.findByUsername(username);
    }

    /**
     * 更新管理员信息
     *
     * @param currentUsername 当前用户名
     * @param newUsername     新用户名
     * @param currentPassword 当前密码
     * @param newPassword     新密码
     * @return 更新结果，包含成功/失败状态和消息
     */
    public UpdateResult updateAdminInfo(String currentUsername, String newUsername, String currentPassword, String newPassword) {
        Optional<AdminUser> userOptional = adminUserRepository.findByUsername(currentUsername);
        
        if (userOptional.isEmpty()) {
            return new UpdateResult(false, "用户不存在");
        }
        
        AdminUser user = userOptional.get();
        
        // 验证当前密码
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return new UpdateResult(false, "当前密码不正确");
        }
        
        // 检查新用户名是否已存在（如果要修改用户名）
        if (!currentUsername.equals(newUsername)) {
            Optional<AdminUser> existingUser = adminUserRepository.findByUsername(newUsername);
            if (existingUser.isPresent()) {
                return new UpdateResult(false, "用户名已存在");
            }
            user.setUsername(newUsername);
        }
        
        // 更新密码（如果提供了新密码）
        if (newPassword != null && !newPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        
        adminUserRepository.save(user);
        return new UpdateResult(true, "信息更新成功");
    }
    
    /**
     * 更新结果类
     */
    public static class UpdateResult {
        private final boolean success;
        private final String message;
        
        public UpdateResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
} 