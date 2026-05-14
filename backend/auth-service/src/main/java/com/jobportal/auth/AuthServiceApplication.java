package com.jobportal.auth;

import com.jobportal.auth.config.DotenvBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class AuthServiceApplication {
    public static void main(String[] args) {
        DotenvBootstrap.load();
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
