package com.github.clashautochange.controller;

import com.github.clashautochange.entity.AdminUser;
import com.github.clashautochange.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final AdminUserService adminUserService;

    @Autowired
    public AuthController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/profile")
    public String profilePage(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<AdminUser> adminUser = adminUserService.getAdminUser(username);
        
        adminUser.ifPresent(user -> model.addAttribute("username", user.getUsername()));
        
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam("currentUsername") String currentUsername,
            @RequestParam("newUsername") String newUsername,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Authentication authentication) {

        // 验证新密码和确认密码是否匹配
        if (newPassword != null && !newPassword.isEmpty()) {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "新密码与确认密码不匹配");
                return "redirect:/profile";
            }
        }

        AdminUserService.UpdateResult result = adminUserService.updateAdminInfo(
                currentUsername, newUsername, currentPassword, newPassword);

        if (result.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", result.getMessage());
            
            // 如果用户名已更改，需要重新登录
            if (!currentUsername.equals(newUsername)) {
                // 登出当前用户
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    new SecurityContextLogoutHandler().logout(request, null, auth);
                }
                return "redirect:/login?logout";
            }
            
            return "redirect:/profile";
        } else {
            redirectAttributes.addFlashAttribute("error", result.getMessage());
            return "redirect:/profile";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, null, auth);
        }
        session.invalidate();
        return "redirect:/login?logout";
    }
} 