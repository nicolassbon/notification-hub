package com.notificationhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class NotificationHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationHubApplication.class, args);
	}

}
