package com.boutique.preorder;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.user.CurrentUserResolver;

@RestController
@RequestMapping("/api/preorders")
public class PreOrderController {

	private final PreOrderService preOrderService;

	private final CurrentUserResolver currentUserResolver;

	public PreOrderController(PreOrderService preOrderService, CurrentUserResolver currentUserResolver) {
		this.preOrderService = preOrderService;
		this.currentUserResolver = currentUserResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PreOrderResponse preOrder(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PreOrderRequest request) {
		return preOrderService.preOrder(currentUserResolver.userId(jwt), request.productId(), request.quantity());
	}

	@GetMapping
	public List<PreOrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
		return preOrderService.preOrdersOf(currentUserResolver.userId(jwt));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		preOrderService.cancel(currentUserResolver.userId(jwt), id);
	}

}
