package com.boutique.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;

// Double protection : la règle d'URL /api/admin/** ET @PreAuthorize (si la règle d'URL disparaît un jour, la méthode reste protégée)
@RestController
@RequestMapping("/api/admin/login-events")
@PreAuthorize("hasRole('ADMIN')")
public class LoginEventController {

	private final AuthService authService;

	public LoginEventController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping
	public PageResponse<LoginEventResponse> findAll(
			@PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return authService.findLoginEvents(pageable);
	}

}
