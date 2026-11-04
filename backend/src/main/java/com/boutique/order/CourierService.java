package com.boutique.order;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.order.CourierExceptions.CourierLockedException;
import com.boutique.order.CourierExceptions.CourierNotFoundException;
import com.boutique.order.CourierExceptions.InactiveCourierException;

@Service
@Transactional(readOnly = true)
public class CourierService {

	private final CourierRepository courierRepository;

	private final DeliveryRepository deliveryRepository;

	private final OrderRepository orderRepository;

	public CourierService(CourierRepository courierRepository, DeliveryRepository deliveryRepository,
			OrderRepository orderRepository) {
		this.courierRepository = courierRepository;
		this.deliveryRepository = deliveryRepository;
		this.orderRepository = orderRepository;
	}

	public List<CourierResponse> findAll() {
		Map<Long, Long> ongoing = courierRepository.countOngoingDeliveries()
			.stream()
			.collect(Collectors.toMap(CourierRepository.OngoingCount::getCourierId, CourierRepository.OngoingCount::getCount));
		return courierRepository.findAllByOrderByActiveDescNameAsc()
			.stream()
			.map((courier) -> CourierResponse.from(courier, ongoing.getOrDefault(courier.getId(), 0L)))
			.toList();
	}

	@Transactional
	public CourierResponse create(CourierRequest request) {
		Courier courier = courierRepository.save(new Courier(request.name().strip(), request.phone().strip()));
		return CourierResponse.from(courier, 0);
	}

	@Transactional
	public CourierResponse update(Long id, CourierRequest request) {
		Courier courier = getCourier(id);
		courier.update(request.name().strip(), request.phone().strip(), request.active());
		return findAll().stream().filter((c) -> c.id().equals(id)).findFirst().orElseThrow();
	}

	// Affecter (ou retirer avec null) le livreur d'une commande en cours ; jamais un livreur désactivé
	@Transactional
	public void assign(Long orderId, Long courierId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
		if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
			throw new CourierLockedException(orderId);
		}
		Delivery delivery = deliveryRepository.findByOrderId(orderId)
			.orElseThrow(() -> new DeliveryNotFoundException(orderId));
		Courier courier = (courierId == null) ? null : getCourier(courierId);
		if (courier != null && !courier.isActive()) {
			throw new InactiveCourierException(courier.getName());
		}
		delivery.assign(courier);
	}

	private Courier getCourier(Long id) {
		return courierRepository.findById(id).orElseThrow(() -> new CourierNotFoundException(id));
	}

}
