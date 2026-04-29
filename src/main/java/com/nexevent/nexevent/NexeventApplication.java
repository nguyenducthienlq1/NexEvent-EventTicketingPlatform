package com.nexevent.nexevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableAsync
public class NexeventApplication {
	public static void main(String[] args) {
		SpringApplication.run(NexeventApplication.class, args);
		System.out.println("Open Swagger to test API Booking: http://localhost:8080/swagger-ui/index.html");
	}
}
