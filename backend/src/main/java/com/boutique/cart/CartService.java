package com.boutique.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.catalog.Product;
import com.boutique.catalog.ProductNotFoundException;
import com.boutique.catalog.ProductRepository;
import com.boutique.user.UserRepository;

@Service
@Transactional
public class CartService {

	private final CartRepository cartRepository;

	private final ProductRepository productRepository;

	private final UserRepository userRepository;

	public CartService(CartRepository cartRepository, ProductRepository productRepository,
			UserRepository userRepository) {
		this.cartRepository = cartRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public CartResponse getCart(Long userId) {
		return cartRepository.findByUserId(userId).map(CartResponse::from).orElse(CartResponse.empty());
	}

	public CartResponse addItem(Long userId, CartItemRequest request) {
		Cart cart = cartRepository.findByUserId(userId)
			// Le panier est créé au premier ajout ; getReferenceById évite de recharger l'utilisateur
			.orElseGet(() -> cartRepository.save(new Cart(userRepository.getReferenceById(userId))));
		cart.addItem(getProduct(request.productId()), request.quantity());
		return CartResponse.from(cart);
	}

	public CartResponse changeQuantity(Long userId, Long productId, int quantity) {
		Cart cart = getExistingCart(userId, productId);
		cart.changeQuantity(getProduct(productId), quantity);
		return CartResponse.from(cart);
	}

	public CartResponse removeItem(Long userId, Long productId) {
		Cart cart = getExistingCart(userId, productId);
		cart.removeItem(productId);
		return CartResponse.from(cart);
	}

	public void clear(Long userId) {
		cartRepository.findByUserId(userId).ifPresent(Cart::clear);
	}

	private Cart getExistingCart(Long userId, Long productId) {
		return cartRepository.findByUserId(userId).orElseThrow(() -> new CartItemNotFoundException(productId));
	}

	private Product getProduct(Long productId) {
		return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
	}

}
