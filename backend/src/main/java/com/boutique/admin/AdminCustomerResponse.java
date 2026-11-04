package com.boutique.admin;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.admin.AdminCustomerRepository.CustomerSummary;

// Commandes et montant : commandes annulées exclues. lastOrderAt = null si le client n'a jamais commandé
public record AdminCustomerResponse(Long id, String fullName, String email, Instant createdAt, long orderCount,
		BigDecimal totalSpent, Instant lastOrderAt) {

	static AdminCustomerResponse from(CustomerSummary customer) {
		return new AdminCustomerResponse(customer.getId(), customer.getFullName(), customer.getEmail(),
				customer.getCreatedAt(), customer.getOrderCount(), customer.getTotalSpent(), customer.getLastOrderAt());
	}

}
