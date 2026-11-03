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

	private BigDecimal latitude;

	private BigDecimal longitude;

	private Instant shippedAt;

	private Instant deliveredAt;

	protected Delivery() {
	}

	public Delivery(Order order, String addressLine, String city, String phone) {
		this.order = order;
		this.addressLine = addressLine;
		this.city = city;
		this.phone = phone;
		this.status = DeliveryStatus.PENDING;
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
