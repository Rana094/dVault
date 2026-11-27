package com.dvault.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    public record Health(String status) {}

    @GetMapping("/api/health")
    public Health health() {
        return new Health("UP");
    }
}
