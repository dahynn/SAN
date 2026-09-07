package com.san.api.global.health;

import com.san.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Void> health() {
        return ApiResponse.success();
    }
}
