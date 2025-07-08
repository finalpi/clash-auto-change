package com.github.clashautochange.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    
    private static final String REMEMBER_ME_KEY = UUID.randomUUID().toString();

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService, DataSource dataSource) {
        this.userDetailsService = userDetailsService;
        this.dataSource = dataSource;
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
        tokenRepository.setCreateTableOnStartup(false);
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
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")); // 对API请求禁用CSRF保护
        
        return http.build();
    }
} 