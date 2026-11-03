package com.boutique.cart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;

import com.boutique.catalog.InsufficientStockException;
import com.boutique.catalog.Product;
import com.boutique.user.User;

@Entity
@Table(name = "carts")
public class Cart {

	static final int MAX_QUANTITY_PER_PRODUCT = 99;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private User user;

	@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CartItem> items = new ArrayList<>();

	@UpdateTimestamp
	private Instant updatedAt;

	protected Cart() {
	}

	public Cart(User user) {
		this.user = user;
	}

	// Un produit déjà présent voit sa quantité augmenter (une seule ligne par produit, contrainte UNIQUE en base)
	public void addItem(Product product, int quantity) {
		CartItem item = findItem(product.getId()).orElse(null);
		int newQuantity = (item != null) ? item.getQuantity() + quantity : quantity;
		checkQuantity(product, newQuantity);
		if (item != null) {
			item.changeQuantity(newQuantity);
		}
		else {
			items.add(new CartItem(this, product, quantity));
		}
	}

	public void changeQuantity(Product product, int quantity) {
		CartItem item = findItem(product.getId()).orElseThrow(() -> new CartItemNotFoundException(product.getId()));
		checkQuantity(product, quantity);
		item.changeQuantity(quantity);
	}

	public void removeItem(Long productId) {
		items.removeIf((item) -> item.getProduct().getId().equals(productId));
	}

	// orphanRemoval = true : retirer les lignes de la liste suffit à les supprimer en base
	public void clear() {
		items.clear();
	}

	public boolean isEmpty() {
		return items.isEmpty();
	}

	// Le stock est vérifié dès l'ajout pour prévenir le client ; il sera revérifié au moment de commander
	private static void checkQuantity(Product product, int quantity) {
		if (quantity > MAX_QUANTITY_PER_PRODUCT) {
			throw new CartQuantityTooLargeException(MAX_QUANTITY_PER_PRODUCT);
		}
		if (quantity > product.getStock()) {
			throw new InsufficientStockException(product.getId(), product.getStock(), quantity);
		}
	}

	private Optional<CartItem> findItem(Long productId) {
		return items.stream().filter((item) -> item.getProduct().getId().equals(productId)).findFirst();
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public List<CartItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
