package com.boutique.order;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

	// Passer commande avec le contenu du panier : aucun prix ni montant n'est lu dans la requête
	@PostMapping
	public ResponseEntity<OrderResponse> checkout(@AuthenticationPrincipal Jwt jwt) {
		OrderResponse order = orderService.checkout(currentUserResolver.userId(jwt));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(order.id()).toUri();
		return ResponseEntity.created(location).body(order);
	}

	// Protection IDOR : l'identifiant du client vient du jeton signé, jamais de l'URL ni du corps de la requête
	@GetMapping("/me")
	public List<OrderResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
		return orderService.findOrdersOfUser(currentUserResolver.userId(jwt));
	}

	// Ici l'id de la commande EST dans l'URL : le service vérifie qu'elle appartient bien à l'utilisateur du jeton
	@GetMapping("/{id}")
	public OrderResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return orderService.findOrderOfUser(currentUserResolver.userId(jwt), id);
	}

	@PostMapping("/{id}/cancel")
	public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return orderService.cancelByCustomer(currentUserResolver.userId(jwt), id);
	}

}
