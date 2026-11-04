package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;

// courierName / courierPhone : livreur affecté (null si aucun)
public record DeliveryResponse(DeliveryStatus status, String addressLine, String city, String phone,
		BigDecimal latitude, BigDecimal longitude, Instant shippedAt, Instant deliveredAt, String courierName,
		String courierPhone) {

	// Vue admin : le livreur dès qu'il est affecté
	static DeliveryResponse from(Delivery delivery) {
		return of(delivery, delivery.getCourier());
	}

	// Vue client : le livreur seulement une fois le colis parti (avant, l'affectation peut encore changer)
	static DeliveryResponse forCustomer(Delivery delivery) {
		boolean onTheWay = delivery.getStatus() == DeliveryStatus.SHIPPED || delivery.getStatus() == DeliveryStatus.DELIVERED;
		return of(delivery, onTheWay ? delivery.getCourier() : null);
	}

	private static DeliveryResponse of(Delivery delivery, Courier courier) {
		return new DeliveryResponse(delivery.getStatus(), delivery.getAddressLine(), delivery.getCity(),
				delivery.getPhone(), delivery.getLatitude(), delivery.getLongitude(), delivery.getShippedAt(),
				delivery.getDeliveredAt(), (courier == null) ? null : courier.getName(),
				(courier == null) ? null : courier.getPhone());
	}

}
