package com.boutique.order;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.security.CurrentUser;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	// Protection IDOR : l'identifiant du client vient du jeton signé, jamais de l'URL ni du corps de la requête.
	// Il n'existe aucun moyen de demander « les commandes de l'utilisateur 42 ».
	@GetMapping("/me")
	public List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
		return orderService.findOrdersOfUser(CurrentUser.id(jwt));
	}

}
