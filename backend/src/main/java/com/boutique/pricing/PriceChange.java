package com.boutique.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// Une ligne du journal : le prix d'UN produit avant et après, dans un lot (un même changement sur plusieurs produits)
@Entity
@Table(name = "price_changes")
public class PriceChange {

	public enum Kind {

		// Remise temporaire : prix barré = ancien prix, date de fin obligatoire
		PROMOTION,
		// Nouveau prix permanent
		PRICE

	}

	public enum RevertReason {

		UNDO, EXPIRED,
		// Le prix a été modifié par ailleurs entre-temps : on ne l'écrase pas
		OVERRIDDEN

	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private UUID batchId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private User author;

	@Enumerated(EnumType.STRING)
	private Kind kind;

	private BigDecimal oldPrice;

	private BigDecimal oldCompareAtPrice;

	private BigDecimal newPrice;

	private BigDecimal newCompareAtPrice;

	private Instant endsAt;

	@CreationTimestamp
	private Instant createdAt;

	private Instant revertedAt;

	@Enumerated(EnumType.STRING)
	private RevertReason revertReason;

	protected PriceChange() {
	}

	PriceChange(UUID batchId, Product product, User author, Kind kind, BigDecimal newPrice, BigDecimal newCompareAtPrice,
			Instant endsAt) {
		this.batchId = batchId;
		this.product = product;
		this.author = author;
		this.kind = kind;
		this.oldPrice = product.getPrice();
		this.oldCompareAtPrice = product.getCompareAtPrice();
		this.newPrice = newPrice;
		this.newCompareAtPrice = newCompareAtPrice;
		this.endsAt = endsAt;
	}

	// Le produit porte-t-il encore exactement les prix posés par ce changement ?
	boolean isStillInPlace() {
		return product.getPrice().compareTo(newPrice) == 0 && sameAmount(product.getCompareAtPrice(), newCompareAtPrice);
	}

	void revert(RevertReason reason, Instant now) {
		if (reason != RevertReason.OVERRIDDEN) {
			product.reprice(oldPrice, oldCompareAtPrice);
		}
		this.revertReason = reason;
		this.revertedAt = now;
	}

	private static boolean sameAmount(BigDecimal a, BigDecimal b) {
		return (a == null || b == null) ? Objects.equals(a, b) : a.compareTo(b) == 0;
	}

	public Long getId() {
		return id;
	}

	public UUID getBatchId() {
		return batchId;
	}

	public Product getProduct() {
		return product;
	}

	public User getAuthor() {
		return author;
	}

	public Kind getKind() {
		return kind;
	}

	public BigDecimal getOldPrice() {
		return oldPrice;
	}

	public BigDecimal getOldCompareAtPrice() {
		return oldCompareAtPrice;
	}

	public BigDecimal getNewPrice() {
		return newPrice;
	}

	public BigDecimal getNewCompareAtPrice() {
		return newCompareAtPrice;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getRevertedAt() {
		return revertedAt;
	}

	public RevertReason getRevertReason() {
		return revertReason;
	}

}
