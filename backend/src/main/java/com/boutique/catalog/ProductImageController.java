package com.boutique.catalog;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// Photos d'un produit : écritures réservées à l'ADMIN, comme le reste du catalogue.
// Taille maximale d'un fichier : spring.servlet.multipart.max-file-size (au-delà : 413)
@RestController
@RequestMapping("/api/products/{productId}/images")
@PreAuthorize("hasRole('ADMIN')")
public class ProductImageController {

	private final ProductImageService productImageService;

	public ProductImageController(ProductImageService productImageService) {
		this.productImageService = productImageService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse upload(@PathVariable Long productId, @RequestParam("file") MultipartFile file)
			throws IOException {
		if (file.isEmpty()) {
			throw new InvalidImageException("Le fichier est vide.");
		}
		return productImageService.addImage(productId, file.getBytes());
	}

	@PutMapping("/{imageId}/main")
	public ProductResponse makeMain(@PathVariable Long productId, @PathVariable Long imageId) {
		return productImageService.makeMainImage(productId, imageId);
	}

	@DeleteMapping("/{imageId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long productId, @PathVariable Long imageId) {
		productImageService.deleteImage(productId, imageId);
	}

}
