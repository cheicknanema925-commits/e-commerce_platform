package com.boutique.agent;

import java.util.Map;

// Une étape d'une action proposée, ex. { type: ADD_TO_CART, params: {productId: 4, quantity: 2}, summary: "Ajouter 2 × …" }.
// Elle n'est exécutée que si l'utilisateur confirme.
public record ProposalStep(String type, Map<String, Object> params, String summary) {
}
