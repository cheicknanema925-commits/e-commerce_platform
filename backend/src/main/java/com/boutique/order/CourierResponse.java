package com.boutique.order;

// ongoingDeliveries : commandes affectées pas encore livrées
public record CourierResponse(Long id, String name, String phone, boolean active, long ongoingDeliveries) {

	static CourierResponse from(Courier courier, long ongoingDeliveries) {
		return new CourierResponse(courier.getId(), courier.getName(), courier.getPhone(), courier.isActive(),
				ongoingDeliveries);
	}

}
