package com.example.datalake.ingestionsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class IngestionSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(IngestionSvcApplication.class, args);
	}

}
