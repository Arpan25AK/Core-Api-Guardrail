package com.assignment.Core_Api_Guardrails;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreApiGuardrailsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreApiGuardrailsApplication.class, args);
	}

}
