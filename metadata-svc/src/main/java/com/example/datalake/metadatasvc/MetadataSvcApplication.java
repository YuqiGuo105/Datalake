package com.example.datalake.metadatasvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AwsProps.class)
public class MetadataSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetadataSvcApplication.class, args);
	}

}
