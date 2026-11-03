package com.boutique.payment;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

// Un seul fournisseur actif, choisi par boutique.payment.provider (fake ou paydunya)
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PaymentProperties.class)
class PaymentConfig {

	// Le faux fournisseur n'existe jamais en production : avec provider=fake et le profil prod, l'appli refuse de démarrer
	@Bean
	@Profile("!prod")
	@ConditionalOnProperty(name = "boutique.payment.provider", havingValue = "fake", matchIfMissing = true)
	FakePaymentProvider fakePaymentProvider(PaymentProperties properties) {
		return new FakePaymentProvider(properties.fake().webhookSecret(),
				properties.publicApiUrl() + "/api/dev/fake-payments/");
	}

	// Délais maximum : si PayDunya ne répond pas, on ne bloque pas un fil d'exécution indéfiniment
	@Bean
	@ConditionalOnProperty(name = "boutique.payment.provider", havingValue = "paydunya")
	PayDunyaPaymentProvider payDunyaPaymentProvider(PaymentProperties properties) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
		requestFactory.setReadTimeout(Duration.ofSeconds(15));
		return new PayDunyaPaymentProvider(RestClient.builder().requestFactory(requestFactory), properties.paydunya(),
				properties.storeName());
	}

	// Tâches planifiées (réconciliation) : désactivables, par exemple dans les tests
	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(name = "boutique.payment.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
	static class SchedulingConfig {

	}

}
