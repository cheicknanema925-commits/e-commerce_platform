package com.boutique.newsletter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

	// ON CONFLICT DO NOTHING (PostgreSQL) : deux inscriptions simultanées de la même adresse ne provoquent ni doublon
	// ni erreur. Renvoie 1 si l'adresse vient d'être inscrite, 0 si elle l'était déjà.
	@Modifying
	@Query(value = """
			insert into newsletter_subscribers (email, unsubscribe_token) values (:email, :token)
			on conflict (email) do nothing
			""", nativeQuery = true)
	int insertIfAbsent(String email, String token);

	@Modifying
	@Query("delete from NewsletterSubscriber s where s.unsubscribeToken = :token")
	int deleteByUnsubscribeToken(String token);

}
