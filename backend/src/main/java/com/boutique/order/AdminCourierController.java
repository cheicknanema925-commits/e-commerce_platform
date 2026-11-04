package com.boutique.order;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Livreurs : liste, ajout, modification (désactivation comprise), affectation à une commande
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourierController {

	private final CourierService courierService;

	public AdminCourierController(CourierService courierService) {
		this.courierService = courierService;
	}

	@GetMapping("/api/admin/couriers")
	public List<CourierResponse> findAll() {
		return courierService.findAll();
	}

	@PostMapping("/api/admin/couriers")
	@ResponseStatus(HttpStatus.CREATED)
	public CourierResponse create(@Valid @RequestBody CourierRequest request) {
		return courierService.create(request);
	}

	@PutMapping("/api/admin/couriers/{id}")
	public CourierResponse update(@PathVariable Long id, @Valid @RequestBody CourierRequest request) {
		return courierService.update(id, request);
	}

	@PutMapping("/api/admin/orders/{orderId}/courier")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void assign(@PathVariable Long orderId, @RequestBody AssignCourierRequest request) {
		courierService.assign(orderId, request.courierId());
	}

}
