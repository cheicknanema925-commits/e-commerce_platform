package com.boutique.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "products")
public class Product {

	static final int MAX_IMAGES = 8;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id")
	private Category category;

	private String name;

	private String description;

	private BigDecimal price;

	private int stock;

	// Verrouillage optimiste : Hibernate vérifie et incrémente ce numéro à chaque UPDATE
	@Version
	private long version;

	// Photos triées, la première est la principale.
	// @BatchSize : pour une page de 20 produits, les photos des 20 arrivent en UNE requête (product_id in (...)),
	// pas une par produit (N+1). @OptimisticLock(excluded) : ajouter une photo ne change pas la version du produit,
	// donc ne fait pas échouer l'achat d'un client au même moment.
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	@BatchSize(size = 100)
	@OptimisticLock(excluded = true)
	private List<ProductImage> images = new ArrayList<>();

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	protected Product() {
	}

	public Product(Category category, String name, String description, BigDecimal price, int stock) {
		update(category, name, description, price, stock);
	}

	public void update(Category category, String name, String description, BigDecimal price, int stock) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.price = price;
		this.stock = stock;
	}

	public void removeStock(int quantity) {
		if (quantity > stock) {
			throw new InsufficientStockException(id, stock, quantity);
		}
		stock -= quantity;
	}

	// Remise en stock (commande annulée)
	public void addStock(int quantity) {
		stock += quantity;
	}

	public ProductImage addImage(String filename) {
		if (images.size() >= MAX_IMAGES) {
			throw new TooManyImagesException(MAX_IMAGES);
		}
		ProductImage image = new ProductImage(this, filename, images.size());
		images.add(image);
		return image;
	}

	// Retire la photo et renvoie le nom de son fichier (à effacer du disque après le commit)
	public String removeImage(Long imageId) {
		ProductImage image = findImage(imageId);
		images.remove(image);
		renumberImages();
		return image.getFilename();
	}

	public void makeMainImage(Long imageId) {
		ProductImage image = findImage(imageId);
		images.remove(image);
		images.add(0, image);
		renumberImages();
	}

	private ProductImage findImage(Long imageId) {
		return images.stream()
			.filter((image) -> image.getId().equals(imageId))
			.findFirst()
			.orElseThrow(() -> new ProductImageNotFoundException(String.valueOf(imageId)));
	}

	private void renumberImages() {
		for (int i = 0; i < images.size(); i++) {
			images.get(i).moveTo(i);
		}
	}

	public Long getId() {
		return id;
	}

	public Category getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getStock() {
		return stock;
	}

	// URL de la photo principale, ou null si le produit n'a pas encore de photo
	public String getMainImageUrl() {
		return images.isEmpty() ? null : images.getFirst().getUrl();
	}

	public List<ProductImage> getImages() {
		return Collections.unmodifiableList(images);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
