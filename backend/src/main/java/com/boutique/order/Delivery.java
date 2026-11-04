package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "deliveries")
public class Delivery {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@Enumerated(EnumType.STRING)
	private DeliveryStatus status;

	private String addressLine;

	private String city;

	private String phone;

	// Point GPS facultatif donné par le client (utile quand l'adresse seule ne suffit pas au livreur)
	private BigDecimal latitude;

	private BigDecimal longitude;

	private Instant shippedAt;

	private Instant deliveredAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "courier_id")
	private Courier courier;

	protected Delivery() {
	}

	public Delivery(Order order, String addressLine, String city, String phone, BigDecimal latitude,
			BigDecimal longitude) {
		this.order = order;
		this.addressLine = addressLine;
		this.city = city;
		this.phone = phone;
		this.latitude = latitude;
		this.longitude = longitude;
		this.status = DeliveryStatus.PENDING;
	}

	// La livraison suit la commande : la machine à états de la commande reste la seule source de vérité
	void follow(OrderStatus orderStatus, Instant now) {
		switch (orderStatus) {
			case PREPARING -> status = DeliveryStatus.PREPARING;
			case SHIPPED -> {
				status = DeliveryStatus.SHIPPED;
				shippedAt = now;
			}
			case DELIVERED -> {
				status = DeliveryStatus.DELIVERED;
				deliveredAt = now;
			}
			default -> {
				// CREATED, PAID, CANCELLED : rien ne bouge côté livraison
			}
		}
	}

	// null = retirer le livreur
	void assign(Courier courier) {
		this.courier = courier;
	}

	public Courier getCourier() {
		return courier;
	}

	public Long getId() {
		return id;
	}

	public Order getOrder() {
		return order;
	}

	public DeliveryStatus getStatus() {
		return status;
	}

	public String getAddressLine() {
		return addressLine;
	}

	public String getCity() {
		return city;
	}

	public String getPhone() {
		return phone;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public Instant getShippedAt() {
		return shippedAt;
	}

	public Instant getDeliveredAt() {
		return deliveredAt;
	}

}
