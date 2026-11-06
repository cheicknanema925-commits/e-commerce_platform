package com.boutique.preorder;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.notification.NotificationSender;
import com.boutique.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class PreOrderService {

	private final PreOrderRepository preOrderRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	private final NotificationSender notificationSender;

	public PreOrderService(PreOrderRepository preOrderRepository, ProductRepository productRepository,
			UserRepository userRepository, NotificationSender notificationSender) {
		this.preOrderRepository = preOrderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.notificationSender = notificationSender;
	}

	// Règles vérifiées sans rien enregistrer (l'assistant les vérifie avant de proposer) ; renvoie le nom du produit
	public String checkCanPreOrder(Long productId, int quantity) {
		return getOutOfStockProduct(productId, quantity).getName();
	}

	// Idempotent : une deuxième demande pour le même produit met à jour la quantité au lieu d'en créer une autre
	@Transactional
	public PreOrderResponse preOrder(Long userId, Long productId, int quantity) {
		Product product = getOutOfStockProduct(productId, quantity);
		PreOrder preOrder = preOrderRepository
			.findByUserIdAndProductIdAndStatus(userId, productId, PreOrder.Status.WAITING)
			.orElseGet(() -> preOrderRepository.save(new PreOrder(userRepository.getReferenceById(userId), product, quantity)));
		preOrder.changeQuantity(quantity);
		return PreOrderResponse.from(preOrder);
	}

	public List<PreOrderResponse> preOrdersOf(Long userId) {
		return preOrderRepository.findWithProductByUserId(userId).stream().map(PreOrderResponse::from).toList();
	}

	@Transactional
	public void cancel(Long userId, Long preOrderId) {
		preOrderRepository.findByIdAndUserId(preOrderId, userId)
			.orElseThrow(() -> new PreOrderNotFoundException(preOrderId))
			.cancel();
	}

	public List<PreOrderRepository.WaitingDemand> waitingDemand() {
		return preOrderRepository.demandByStatus(PreOrder.Status.WAITING);
	}

	// Appelé APRÈS le commit du réassort (BackInStockListener). REQUIRES_NEW indispensable : avec REQUIRED, ces
	// écritures rejoindraient la transaction déjà validée et ne seraient jamais enregistrées
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public int notifyBackInStock(Long productId) {
		List<PreOrder> waiting = preOrderRepository.findWithUserByProductIdAndStatus(productId, PreOrder.Status.WAITING);
		Instant now = Instant.now();
		for (PreOrder preOrder : waiting) {
			// Pas de réservation : le message le dit, le premier qui commande est servi
			notificationSender.send(preOrder.getUser().getEmail(), preOrder.getProduct().getName() + " est de retour",
					"Bonne nouvelle : « " + preOrder.getProduct().getName() + " » que vous attendiez est de nouveau en stock. "
							+ "Le stock n'est pas réservé : commandez vite sur Shop Africa.");
			preOrder.markNotified(now);
		}
		return waiting.size();
	}

	private Product getOutOfStockProduct(Long productId, int quantity) {
		if (quantity < 1 || quantity > 99) {
			throw new InvalidPreOrderException();
		}
		Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
		if (product.getStock() > 0) {
			throw new ProductAvailableException(product.getName());
		}
		return product;
	}

}
