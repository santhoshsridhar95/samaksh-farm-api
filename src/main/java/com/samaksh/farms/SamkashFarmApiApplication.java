package com.samaksh.farms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SamkashFarmApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SamkashFarmApiApplication.class, args);
	}

}
