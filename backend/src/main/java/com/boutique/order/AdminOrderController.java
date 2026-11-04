package com.boutique.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.common.PageResponse;

// Back-office : suivi et préparation des commandes (règle d'URL /api/admin/** + @PreAuthorize)
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

	private final OrderService orderService;

	public AdminOrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public PageResponse<AdminOrderResponse> findAll(@RequestParam(required = false) OrderStatus status,
			@RequestParam(required = false) PaymentMethod paymentMethod,
			@RequestParam(required = false) @Size(max = 100) String q,
			@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
		return orderService.findAll(status, paymentMethod, q, pageable);
	}

	@GetMapping("/{id}")
	public AdminOrderResponse findOne(@PathVariable Long id) {
		return orderService.findForAdmin(id);
	}

	@GetMapping("/{id}/delivery")
	public DeliveryResponse delivery(@PathVariable Long id) {
		return orderService.findDeliveryForAdmin(id);
	}

	// La machine à états refuse toute transition non prévue (409), par exemple PAID → DELIVERED
	@PatchMapping("/{id}/status")
	public AdminOrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
		return orderService.changeStatusAsAdmin(id, request.status());
	}

}
