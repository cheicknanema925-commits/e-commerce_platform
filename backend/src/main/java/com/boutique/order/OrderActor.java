package com.boutique.order;

// Qui demande le changement de statut : la machine à états vérifie qu'il en a le droit
public enum OrderActor {

	CUSTOMER, ADMIN, PAYMENT_PROVIDER

}
