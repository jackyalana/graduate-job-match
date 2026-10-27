package com.lanrecruitment.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final Environment env;
    public WebMvcConfig(Environment env) { this.env = env; }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
          .allowedOrigins(env.getProperty("lan.cors.origins", "http://127.0.0.1:15173,http://127.0.0.1:15174,http://localhost:5173").split(","))
          .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
          .allowedHeaders("Content-Type", "satoken")
          .exposedHeaders("X-Request-Id", "Content-Disposition")
          .allowCredentials(false);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path base = Paths.get(env.getProperty("lan.storage.base-dir", System.getProperty("user.home") + "/lan-recruitment/uploads")).toAbsolutePath().normalize();
        Path avatar = resolve(base, env.getProperty("lan.storage.avatar"), "avatar");
        Path resume = resolve(base, env.getProperty("lan.storage.resume"), "resume");
        // Only avatars may be static. Reject overlapping configured roots, including symlink aliases.
        try {
            java.nio.file.Files.createDirectories(avatar);
            java.nio.file.Files.createDirectories(resume);
            avatar = avatar.toRealPath(); resume = resume.toRealPath();
        } catch (java.io.IOException e) { throw new IllegalStateException("Storage directories unavailable", e); }
        if (avatar.startsWith(resume) || resume.startsWith(avatar)) throw new IllegalStateException("Avatar and resume paths overlap");
        String location = avatar.toUri().toString();
        registry.addResourceHandler("/uploads/avatar/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
    private Path resolve(Path base, String value, String fallback) {
        Path p = value == null || value.trim().isEmpty() ? base.resolve(fallback) : Paths.get(value);
        return (p.isAbsolute() ? p : base.resolve(p)).toAbsolutePath().normalize();
    }
}
