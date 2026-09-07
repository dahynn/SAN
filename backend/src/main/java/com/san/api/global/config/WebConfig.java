package com.san.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 및 MVC 전역 설정.
 *
 * 프론트엔드 개발 서버(localhost:3000, 5173 등)에서의 요청을 허용합니다.
 * 프로덕션 배포 시 allowedOrigins를 실제 도메인으로 제한해야 합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        addApiCorsMapping(registry, "/auth/**");
        addApiCorsMapping(registry, "/async-jobs/**");
        addApiCorsMapping(registry, "/cards/**");
        addApiCorsMapping(registry, "/github/**");
        addApiCorsMapping(registry, "/scraps/**");
        addApiCorsMapping(registry, "/search/**");
        addApiCorsMapping(registry, "/til/**");
    }

    private void addApiCorsMapping(CorsRegistry registry, String pathPattern) {
        registry.addMapping(pathPattern)
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
