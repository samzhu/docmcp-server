package io.github.samzhu.docmcp.config;

import io.github.samzhu.docmcp.security.ApiKeyAuthenticationFilter;
import io.github.samzhu.docmcp.security.ConcurrencyLimitInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全配置
 * <p>
 * 配置 API Key 認證和併發限制。
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final ConcurrencyLimitInterceptor concurrencyLimitInterceptor;

    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                          ConcurrencyLimitInterceptor concurrencyLimitInterceptor) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
        this.concurrencyLimitInterceptor = concurrencyLimitInterceptor;
    }

    /**
     * MCP 端點的安全配置（需要認證）
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "docmcp.security.api-key.enabled", havingValue = "true")
    public SecurityFilterChain mcpSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/mcp/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * API 端點的安全配置
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // API Key 管理端點需要認證
                        .requestMatchers("/api/keys/**").authenticated()
                        // 其他 API 端點允許匿名存取（可以透過配置啟用認證）
                        .anyRequest().permitAll()
                )
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Web 頁面的安全配置
     */
    @Bean
    @Order(3)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())  // 簡化配置，實際生產環境應啟用
                .authorizeHttpRequests(auth -> auth
                        // 靜態資源允許匿名存取
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // Actuator 端點
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Web 頁面允許匿名存取
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * 註冊併發限制攔截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(concurrencyLimitInterceptor)
                .addPathPatterns("/mcp/**", "/api/**")
                .excludePathPatterns("/api/keys/**");  // API Key 管理端點不限制
    }
}
