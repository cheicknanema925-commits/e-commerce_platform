package com.boutique.user;

import java.time.Instant;

public record LoginEventResponse(Long id, String email, Long userId, String ipAddress, LoginOutcome outcome,
		Instant createdAt) {

	static LoginEventResponse from(LoginEvent event) {
		return new LoginEventResponse(event.getId(), event.getEmail(), event.getUserId(), event.getIpAddress(),
				event.getOutcome(), event.getCreatedAt());
	}

}
