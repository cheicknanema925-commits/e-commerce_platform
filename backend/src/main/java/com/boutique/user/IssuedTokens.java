package com.boutique.user;

// Les deux jetons produits par AuthService. Seul le jeton d'accès part dans le corps de la réponse :
// le refresh token voyage uniquement dans un cookie httpOnly (voir AuthController)
public record IssuedTokens(String accessToken, long expiresIn, String refreshToken) {

}
