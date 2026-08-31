package com.cunoc.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RestaurantApplication
{
	public static void main(String[] args)
	{
		SpringApplication.run(RestaurantApplication.class, args);
	}
}
