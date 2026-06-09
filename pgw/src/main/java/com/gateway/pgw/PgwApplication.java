package com.gateway.pgw;

import java.util.Objects;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.gateway")
public class PgwApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(PgwApplication.class, args);
		
	}




























	public int hash() {
		final String name = "Ifeoma";
		int code = Objects.hash(name);
		System.out.println(code);
		
		return code;
	}

}
