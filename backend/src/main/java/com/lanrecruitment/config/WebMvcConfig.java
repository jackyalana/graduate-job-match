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
    private static final String DEFAULT_BASE_DIR = System.getProperty("user.home") + "/lan-recruitment/uploads";

    public WebMvcConfig(Environment env) {
        this.env = env;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path basePath = Paths.get(env.getProperty("lan.storage.base-dir", DEFAULT_BASE_DIR)).toAbsolutePath().normalize();
        Path avatarPath = resolveDir(basePath, env.getProperty("lan.storage.avatar"), basePath.resolve("avatar"));
        Path resumePath = resolveDir(basePath, env.getProperty("lan.storage.resume"), basePath.resolve("resume"));

        registry.addResourceHandler("/uploads/avatar/**")
                .addResourceLocations(toLocation(avatarPath));
        registry.addResourceHandler("/uploads/resume/**")
                .addResourceLocations(toLocation(resumePath));
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(toLocation(basePath));
    }

    private Path resolveDir(Path basePath, String configured, Path defaultDir) {
        if (configured == null || configured.trim().isEmpty()) {
            return defaultDir.toAbsolutePath().normalize();
        }
        Path p = Paths.get(configured);
        if (!p.isAbsolute()) {
            p = basePath.resolve(configured);
        }
        return p.toAbsolutePath().normalize();
    }

    private String toLocation(Path basePath) {
        String location = basePath.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        return location;
    }
}
