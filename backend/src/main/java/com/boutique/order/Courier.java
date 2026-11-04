package com.boutique.order;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "couriers")
public class Courier {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String phone;

	// Désactivé = ne reçoit plus de nouvelles commandes, mais reste dans l'historique des livraisons
	private boolean active = true;

	@CreationTimestamp
	private Instant createdAt;

	protected Courier() {
	}

	public Courier(String name, String phone) {
		update(name, phone, true);
	}

	public void update(String name, String phone, boolean active) {
		this.name = name;
		this.phone = phone;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPhone() {
		return phone;
	}

	public boolean isActive() {
		return active;
	}

}
