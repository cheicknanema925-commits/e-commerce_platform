package com.boutique.user;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

	private static final Duration LOCK = Duration.ofMinutes(15);

	private final Instant now = Instant.parse("2026-11-03T10:00:00Z");

	@Test
	void accountIsLockedAfterMaxFailedAttempts() {
		User user = new User("awa@example.com", "Awa Diop", Role.CUSTOMER);

		for (int i = 0; i < 4; i++) {
			user.registerFailedLogin(5, LOCK, now);
		}
		assertThat(user.isLocked(now)).isFalse();

		user.registerFailedLogin(5, LOCK, now);
		assertThat(user.isLocked(now)).isTrue();
	}

	@Test
	void lockExpiresAfterLockDuration() {
		User user = new User("awa@example.com", "Awa Diop", Role.CUSTOMER);
		for (int i = 0; i < 5; i++) {
			user.registerFailedLogin(5, LOCK, now);
		}

		assertThat(user.isLocked(now.plus(LOCK).minusSeconds(1))).isTrue();
		assertThat(user.isLocked(now.plus(LOCK))).isFalse();
	}

	@Test
	void successfulLoginResetsFailedAttempts() {
		User user = new User("awa@example.com", "Awa Diop", Role.CUSTOMER);
		user.registerFailedLogin(5, LOCK, now);
		user.registerFailedLogin(5, LOCK, now);

		user.registerSuccessfulLogin();

		assertThat(user.getFailedLoginAttempts()).isZero();
	}

}
