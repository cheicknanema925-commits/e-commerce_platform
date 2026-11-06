package com.boutique.pricing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Rétablit les prix à la fin des promotions. Interrupteur propre au module (désactivé dans les tests, qui appellent
// expirePromotions() eux-mêmes) : il ne dépend pas de la configuration des paiements
@Component
@ConditionalOnProperty(name = "boutique.pricing.expiry.enabled", havingValue = "true", matchIfMissing = true)
class PromotionExpiryJob {

	private static final Logger log = LoggerFactory.getLogger(PromotionExpiryJob.class);

	private final PricingService pricingService;

	PromotionExpiryJob(PricingService pricingService) {
		this.pricingService = pricingService;
	}

	@Scheduled(fixedDelayString = "${boutique.pricing.expiry.interval:PT1M}")
	void expirePromotions() {
		int ended = pricingService.expirePromotions();
		if (ended > 0) {
			log.info("{} promotion(s) terminée(s) : prix rétablis", ended);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(name = "boutique.pricing.expiry.enabled", havingValue = "true", matchIfMissing = true)
	static class SchedulingConfig {

	}

}
