package ru.nvkz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.nvkz.domain.User;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.service.UserService;

import java.time.LocalDate;
import java.time.Month;

@SpringBootApplication
public class UserServiceApplication {


	public static void main(String[] args) {
		ConfigurableApplicationContext a = SpringApplication.run(UserServiceApplication.class, args);

		UserService userService = a.getBean("userService", UserService.class);

		var request = new RegistrationRequest("test11@test.com", "pass", "name", "Surname", "", LocalDate.of(1992, Month.FEBRUARY, 23));
	//	User savedUser = userService.create(request).block();

	//	System.out.println(userService.findById(savedUser.id()).block());

	}

}
