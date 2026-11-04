package com.boutique.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProductImageService {

	private final ProductRepository productRepository;

	private final ImageProcessor imageProcessor;

	private final ImageStorage imageStorage;

	private final TransactionTemplate transactionTemplate;

	private final ApplicationEventPublisher events;

	public ProductImageService(ProductRepository productRepository, ImageProcessor imageProcessor,
			ImageStorage imageStorage, TransactionTemplate transactionTemplate, ApplicationEventPublisher events) {
		this.productRepository = productRepository;
		this.imageProcessor = imageProcessor;
		this.imageStorage = imageStorage;
		this.transactionTemplate = transactionTemplate;
		this.events = events;
	}

	// Traitement de l'image (lent) et écriture du fichier HORS transaction : la connexion à la base n'est occupée
	// que le temps de l'INSERT. Si la base refuse (produit inconnu, déjà 8 photos), le fichier est effacé.
	public ProductResponse addImage(Long productId, byte[] content) {
		byte[] jpeg = imageProcessor.toJpeg(content);
		String filename = UUID.randomUUID() + ".jpg";
		imageStorage.save(filename, jpeg);
		try {
			return transactionTemplate.execute((status) -> {
				Product product = getProduct(productId);
				product.addImage(filename);
				// flush : l'INSERT part maintenant, la photo reçoit son id avant qu'on construise la réponse
				productRepository.flush();
				return ProductResponse.from(product);
			});
		}
		catch (RuntimeException ex) {
			imageStorage.delete(filename);
			throw ex;
		}
	}

	@Transactional
	public void deleteImage(Long productId, Long imageId) {
		String filename = getProduct(productId).removeImage(imageId);
		events.publishEvent(new ProductImagesDeletedEvent(List.of(filename)));
	}

	@Transactional
	public ProductResponse makeMainImage(Long productId, Long imageId) {
		Product product = getProduct(productId);
		product.makeMainImage(imageId);
		return ProductResponse.from(product);
	}

	// AFTER_COMMIT : si la transaction échoue (ex. produit déjà commandé, suppression refusée), les fichiers restent
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void deleteFiles(ProductImagesDeletedEvent event) {
		event.filenames().forEach(imageStorage::delete);
	}

	private Product getProduct(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

}
