package com.boutique.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.boutique.catalog.Product;
import com.boutique.user.User;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	private BigDecimal totalAmount;

	// Verrouillage optimiste : empêche par exemple « annulée » et « payée » d'être enregistrées en même temps
	@Version
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	protected Order() {
	}

	public Order(User user) {
		this.user = user;
		this.status = OrderStatus.CREATED;
		this.totalAmount = BigDecimal.ZERO;
	}

	public void addItem(Product product, int quantity) {
		OrderItem item = new OrderItem(this, product, quantity);
		items.add(item);
		totalAmount = totalAmount.add(item.getSubtotal());
	}

	// Seul point d'entrée pour changer de statut : toute transition non prévue par la machine à états est refusée
	public void changeStatus(OrderStatus target, OrderActor actor) {
		if (!status.canTransitionTo(target, actor)) {
			throw new InvalidOrderTransitionException(status, target);
		}
		status = target;
	}

	public boolean belongsTo(Long userId) {
		return user.getId().equals(userId);
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public List<OrderItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
