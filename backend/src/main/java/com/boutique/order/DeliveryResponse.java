package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;

public record DeliveryResponse(DeliveryStatus status, String addressLine, String city, String phone,
		BigDecimal latitude, BigDecimal longitude, Instant shippedAt, Instant deliveredAt) {

	static DeliveryResponse from(Delivery delivery) {
		return new DeliveryResponse(delivery.getStatus(), delivery.getAddressLine(), delivery.getCity(),
				delivery.getPhone(), delivery.getLatitude(), delivery.getLongitude(), delivery.getShippedAt(),
				delivery.getDeliveredAt());
	}

}
