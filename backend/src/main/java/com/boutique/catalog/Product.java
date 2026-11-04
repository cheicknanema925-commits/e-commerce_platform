package com.boutique.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
import jakarta.persistence.PrePersist;
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

	// Fabricant (Samsung, Nike…), facultatif
	private String brand;

	// Référence interne unique (SKU) : générée si l'admin n'en donne pas
	private String sku;

	// Prix barré (prix avant remise) : null = pas de promotion
	private BigDecimal compareAtPrice;

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

	// Référence générée au premier enregistrement si l'admin n'en a pas saisi : SHA- + 8 caractères aléatoires
	@PrePersist
	void generateSkuIfMissing() {
		if (sku == null) {
			sku = "SHA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
		}
	}

	// Marque, référence et prix barré ; la référence n'est changée que si une nouvelle est fournie
	public void updateMerchandising(String brand, String sku, BigDecimal compareAtPrice) {
		if (compareAtPrice != null && compareAtPrice.compareTo(price) <= 0) {
			throw new InvalidPromotionException();
		}
		this.brand = brand;
		if (sku != null) {
			this.sku = sku;
		}
		this.compareAtPrice = compareAtPrice;
	}

	// Remise en pourcentage, arrondie à l'entier inférieur (jamais annoncer plus que la vraie remise) ; null sans promotion
	public Integer getDiscountPercent() {
		if (compareAtPrice == null) {
			return null;
		}
		return BigDecimal.ONE.subtract(price.divide(compareAtPrice, 4, RoundingMode.HALF_UP))
			.multiply(BigDecimal.valueOf(100))
			.setScale(0, RoundingMode.FLOOR)
			.intValue();
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

	public String getBrand() {
		return brand;
	}

	public String getSku() {
		return sku;
	}

	public BigDecimal getCompareAtPrice() {
		return compareAtPrice;
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
