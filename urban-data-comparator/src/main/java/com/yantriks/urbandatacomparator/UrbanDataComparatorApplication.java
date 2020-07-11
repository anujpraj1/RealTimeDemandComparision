package com.yantriks.urbandatacomparator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yantriks.urbandatacomparator"})
public class UrbanDataComparatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrbanDataComparatorApplication.class, args);
	}
}
