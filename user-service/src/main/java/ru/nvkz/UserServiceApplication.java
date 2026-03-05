package ru.nvkz;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.nvkz.domain.User;
import ru.nvkz.domain.UserProfile;
import ru.nvkz.dto.RegistrationRequest;
import ru.nvkz.service.UserService;

@SpringBootApplication
public class UserServiceApplication {


	public static void main(String[] args) {
		ConfigurableApplicationContext a = SpringApplication.run(UserServiceApplication.class, args);

		UserService userService = a.getBean("userService", UserService.class);

		var request = new RegistrationRequest("test5@test.com", "pass", "name", "Surname");
		User savedUser = userService.create(request).block();

		System.out.println(userService.findById(savedUser.id()).block());

	}

}
