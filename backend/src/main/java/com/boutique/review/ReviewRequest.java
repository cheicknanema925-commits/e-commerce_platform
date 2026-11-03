package com.boutique.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// Le commentaire est un texte libre écrit par un client : il ne doit jamais être interprété comme du HTML
// (React l'échappe à l'étape 7) ni comme des instructions (l'agent IA le lira à l'étape 8)
public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 2000) String comment) {

}
