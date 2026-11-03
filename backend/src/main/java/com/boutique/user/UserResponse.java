package com.boutique.user;

// Jamais le hash du mot de passe ni le compteur d'échecs dans une réponse
public record UserResponse(Long id, String email, String fullName, Role role) {

	static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
	}

}
