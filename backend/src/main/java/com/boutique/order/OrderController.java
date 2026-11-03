package com.boutique.order;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.user.CurrentUserResolver;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	private final CurrentUserResolver currentUserResolver;

	public OrderController(OrderService orderService, CurrentUserResolver currentUserResolver) {
		this.orderService = orderService;
		this.currentUserResolver = currentUserResolver;
	}

	// Protection IDOR : l'identifiant du client vient du jeton signé, jamais de l'URL ni du corps de la requête.
	// Il n'existe aucun moyen de demander « les commandes de l'utilisateur 42 ».
	@GetMapping("/me")
	public List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
		return orderService.findOrdersOfUser(currentUserResolver.userId(jwt));
	}

}
