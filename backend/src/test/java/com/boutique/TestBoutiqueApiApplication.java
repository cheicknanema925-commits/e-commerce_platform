package com.boutique;

import org.springframework.boot.SpringApplication;

public class TestBoutiqueApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(BoutiqueApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
