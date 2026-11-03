package com.boutique.cart;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.user.CurrentUserResolver;

// Le panier de l'utilisateur connecté : son identité vient du jeton, il n'y a pas d'id de panier dans l'URL
@RestController
@RequestMapping("/api/cart")
public class CartController {

	private final CartService cartService;

	private final CurrentUserResolver currentUserResolver;

	public CartController(CartService cartService, CurrentUserResolver currentUserResolver) {
		this.cartService = cartService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public CartResponse get(@AuthenticationPrincipal Jwt jwt) {
		return cartService.getCart(currentUserResolver.userId(jwt));
	}

	@PostMapping("/items")
	public CartResponse addItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemRequest request) {
		return cartService.addItem(currentUserResolver.userId(jwt), request);
	}

	@PutMapping("/items/{productId}")
	public CartResponse changeQuantity(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
			@Valid @RequestBody QuantityRequest request) {
		return cartService.changeQuantity(currentUserResolver.userId(jwt), productId, request.quantity());
	}

	@DeleteMapping("/items/{productId}")
	public CartResponse removeItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
		return cartService.removeItem(currentUserResolver.userId(jwt), productId);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clear(@AuthenticationPrincipal Jwt jwt) {
		cartService.clear(currentUserResolver.userId(jwt));
	}

}
