package com.boutique.review;

// Note moyenne arrondie à une décimale (0 s'il n'y a encore aucun avis)
public record RatingResponse(double average, long count) {

}
