package com.github.clashautochange.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    private static final String REMEMBER_ME_KEY = UUID.randomUUID().toString();

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService, DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.userDetailsService = userDetailsService;
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        initRememberMeTable();
    }
    
    /**
     * 初始化Remember Me表
     */
    private void initRememberMeTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS persistent_logins (" +
                    "username VARCHAR(64) NOT NULL, " +
                    "series VARCHAR(64) PRIMARY KEY, " +
                    "token VARCHAR(64) NOT NULL, " +
                    "last_used TIMESTAMP NOT NULL)");
        } catch (Exception e) {
            // 记录错误但不中断应用启动
            System.err.println("初始化Remember Me表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }
    
    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        tokenRepository.setCreateTableOnStartup(false); // 我们已经在构造函数中创建了表
        return tokenRepository;
    }
    
    @Bean
    public RememberMeServices rememberMeServices() {
        PersistentTokenBasedRememberMeServices rememberMeServices = 
                new PersistentTokenBasedRememberMeServices(
                        REMEMBER_ME_KEY, 
                        userDetailsService, 
                        persistentTokenRepository());
        rememberMeServices.setTokenValiditySeconds(604800); // 7天 = 7 * 24 * 60 * 60 = 604800秒
        return rememberMeServices;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/api/**").permitAll() // 允许API接口访问，以便Clash可以调用
                .requestMatchers("/profile").authenticated() // 个人资料页面需要认证
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .rememberMeServices(rememberMeServices())
                .key(REMEMBER_ME_KEY)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // 对API请求禁用CSRF保护
                .ignoringRequestMatchers("/settings/update") // 对设置更新请求禁用CSRF保护
                .ignoringRequestMatchers("/proxy-groups/add") // 对添加代理组请求禁用CSRF保护
                .ignoringRequestMatchers("/proxy-groups/update/**") // 对更新代理组请求禁用CSRF保护
                .ignoringRequestMatchers("/proxy-groups/delete/**") // 对删除代理组请求禁用CSRF保护
                .ignoringRequestMatchers("/proxy-groups/toggle/**") // 对切换代理组状态请求禁用CSRF保护
                .ignoringRequestMatchers("/profile") // 对个人资料更新请求禁用CSRF保护
            );
        
        return http.build();
    }
} 