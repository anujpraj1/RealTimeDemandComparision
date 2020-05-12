package com.yantriks.urbandatacomparator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.yantriks"})
public class UrbanDataComparatorApplication {

	public static void main(String[] args) {
		for (String arg: args) {
			System.out.println("Arg :: "+arg);
		}
		SpringApplication.run(UrbanDataComparatorApplication.class, args);
	}

}
