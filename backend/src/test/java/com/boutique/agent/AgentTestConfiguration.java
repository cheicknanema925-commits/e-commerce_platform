package com.boutique.agent;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Remplace le modèle Ollama par le faux modèle scénarisé : tests rapides, déterministes, sans réseau
@TestConfiguration(proxyBeanMethods = false)
public class AgentTestConfiguration {

	@Bean
	@Primary
	ScriptedChatModel scriptedChatModel() {
		return new ScriptedChatModel();
	}

}
