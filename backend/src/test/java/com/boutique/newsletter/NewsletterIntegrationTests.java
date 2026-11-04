package com.boutique.newsletter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.boutique.TestcontainersConfiguration;
import com.boutique.notification.NotificationSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class NewsletterIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NewsletterSubscriberRepository repository;

	// Faux expéditeur : on vérifie le message envoyé sans rien envoyer
	@MockitoBean
	private NotificationSender notificationSender;

	@AfterEach
	void cleanUp() {
		repository.deleteAll();
	}

	// Inscrite ou déjà inscrite : même réponse (personne ne peut savoir qui est abonné), un seul message de bienvenue
	@Test
	void subscribingTwiceGivesTheSameAnswerAndASingleSubscription() throws Exception {
		subscribe("Awa@Example.com").andExpect(status().isAccepted());
		subscribe("awa@example.com").andExpect(status().isAccepted());

		assertThat(repository.findAll()).extracting(NewsletterSubscriber::getEmail).containsExactly("awa@example.com");
		verify(notificationSender, times(1)).send(eq("awa@example.com"), anyString(), contains("/desinscription?jeton="));
	}

	@Test
	void unsubscribeWorksWithTheSecretTokenOnly() throws Exception {
		subscribe("awa@example.com");
		String token = repository.findAll().getFirst().getUnsubscribeToken();

		// Jeton inconnu : même réponse, rien n'est supprimé
		unsubscribe("jeton-invente").andExpect(status().isNoContent());
		assertThat(repository.count()).isEqualTo(1);

		unsubscribe(token).andExpect(status().isNoContent());
		assertThat(repository.count()).isZero();
	}

	@Test
	void invalidEmailIsRefused() throws Exception {
		subscribe("pas-un-email").andExpect(status().isBadRequest());
	}

	// La liste admin ne montre jamais le jeton de désinscription ; un client n'y a pas accès
	@Test
	void adminSeesSubscribersWithoutTheirToken() throws Exception {
		subscribe("awa@example.com");

		mockMvc.perform(get("/api/admin/newsletter").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].email").value("awa@example.com"))
			.andExpect(jsonPath("$.content[0].unsubscribeToken").doesNotExist());
		mockMvc.perform(get("/api/admin/newsletter").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
			.andExpect(status().isForbidden());
	}

	private ResultActions subscribe(String email) throws Exception {
		return mockMvc.perform(post("/api/newsletter/subscriptions").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\": \"%s\"}".formatted(email)));
	}

	private ResultActions unsubscribe(String token) throws Exception {
		return mockMvc.perform(post("/api/newsletter/unsubscribe").contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\": \"%s\"}".formatted(token)));
	}

}
