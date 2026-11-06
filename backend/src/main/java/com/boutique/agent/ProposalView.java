package com.boutique.agent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Ce que l'interface affiche : la liste des actions à confirmer, et jusqu'à quand
public record ProposalView(UUID id, List<String> steps, Instant expiresAt) {
}
