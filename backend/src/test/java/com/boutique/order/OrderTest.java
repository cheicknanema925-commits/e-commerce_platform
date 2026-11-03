package com.boutique.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.boutique.catalog.Category;
import com.boutique.catalog.Product;
import com.boutique.user.Role;
import com.boutique.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

	private final Category mode = new Category("Mode", "mode");

	private final User customer = new User("awa@example.com", "Awa Diop", Role.CUSTOMER);

	@Test
	void addItemComputesTotal() {
		Order order = new Order(customer);

		order.addItem(new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5), 2);
		order.addItem(new Product(mode, "Sandales cuir", null, new BigDecimal("8000"), 5), 1);

		assertThat(order.getTotalAmount()).isEqualByComparingTo("58000");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
	}

	@Test
	void forbiddenTransitionIsRefusedAndStatusIsUnchanged() {
		Order order = new Order(customer);

		assertThatThrownBy(() -> order.changeStatus(OrderStatus.DELIVERED, OrderActor.ADMIN))
			.isInstanceOf(InvalidOrderTransitionException.class);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
	}

	@Test
	void priceChangeAfterPurchaseDoesNotChangeOrder() {
		Product boubou = new Product(mode, "Boubou bazin", null, new BigDecimal("25000"), 5);
		Order order = new Order(customer);
		order.addItem(boubou, 1);

		boubou.update(mode, "Boubou bazin", null, new BigDecimal("40000"), 5);

		assertThat(order.getItems().get(0).getUnitPrice()).isEqualByComparingTo("25000");
		assertThat(order.getTotalAmount()).isEqualByComparingTo("25000");
	}

}
