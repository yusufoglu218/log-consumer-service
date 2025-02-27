package com.kafein.consumer.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.kafein.common.config", "com.kafein.common.exception", "com.kafein.consumer"})
public class LogConsumerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogConsumerServiceApplication.class, args);
	}

}
