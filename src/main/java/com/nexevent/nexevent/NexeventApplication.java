package com.nexevent.nexevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexeventApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexeventApplication.class, args);
		System.out.println("Open Swagger to test API Booking: http://localhost:8080/swagger-ui/index.html");
	}
}
