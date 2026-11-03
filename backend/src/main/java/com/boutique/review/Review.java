package com.boutique.review;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import com.boutique.catalog.Product;
import com.boutique.user.User;

@Entity
@Table(name = "reviews")
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	private int rating;

	private String comment;

	@CreationTimestamp
	private Instant createdAt;

	protected Review() {
	}

	public Review(User user, Product product, int rating, String comment) {
		this.user = user;
		this.product = product;
		this.rating = rating;
		this.comment = comment;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Product getProduct() {
		return product;
	}

	public int getRating() {
		return rating;
	}

	public String getComment() {
		return comment;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
