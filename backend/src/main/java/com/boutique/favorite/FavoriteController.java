package com.boutique.favorite;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.boutique.catalog.ProductResponse;
import com.boutique.user.CurrentUserResolver;

// Favoris du client connecté : l'identité vient TOUJOURS du jeton (jamais d'id de client dans l'URL : IDOR)
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

	private final FavoriteService favoriteService;

	private final CurrentUserResolver currentUserResolver;

	public FavoriteController(FavoriteService favoriteService, CurrentUserResolver currentUserResolver) {
		this.favoriteService = favoriteService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public List<ProductResponse> favorites(@AuthenticationPrincipal Jwt jwt) {
		return favoriteService.favorites(currentUserResolver.userId(jwt));
	}

	// Seulement les ids : de quoi colorer les cœurs du catalogue sans recharger tous les produits
	@GetMapping("/ids")
	public List<Long> favoriteIds(@AuthenticationPrincipal Jwt jwt) {
		return favoriteService.favoriteProductIds(currentUserResolver.userId(jwt));
	}

	@PutMapping("/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void add(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
		favoriteService.add(currentUserResolver.userId(jwt), productId);
	}

	@DeleteMapping("/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
		favoriteService.remove(currentUserResolver.userId(jwt), productId);
	}

}
