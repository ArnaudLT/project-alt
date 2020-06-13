package com.arnaudlt.projectalt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages= {"com.arnaudlt.projectalt"})
public class ProjectAltApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectAltApplication.class, args);
	}

}
