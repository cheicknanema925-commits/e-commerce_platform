package com.boutique.order;

import com.boutique.common.ConflictException;

public class InvalidOrderTransitionException extends ConflictException {

	public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
		super("Changement de statut impossible : " + from + " → " + to + ".");
	}

}
