package com.jobportal.job;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication @EnableDiscoveryClient @EnableFeignClients @EnableCaching @EnableScheduling
public class JobServiceApplication {
    public static void main(String[] args) { SpringApplication.run(JobServiceApplication.class, args); }
}
