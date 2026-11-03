package com.boutique.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;

	private final CurrentUserResolver currentUserResolver;

	public UserController(UserService userService, CurrentUserResolver currentUserResolver) {
		this.userService = userService;
		this.currentUserResolver = currentUserResolver;
	}

	// @AuthenticationPrincipal : le jeton déjà vérifié par Spring Security (le nôtre ou celui de Keycloak)
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return userService.getProfile(currentUserResolver.userId(jwt));
	}

}
