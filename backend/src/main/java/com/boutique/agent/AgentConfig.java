package com.boutique.agent;

import java.util.Arrays;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableAsync;

import tools.jackson.databind.json.JsonMapper;

import com.boutique.agent.admin.AdminTools;
import com.boutique.agent.client.ClientTools;
import com.boutique.agent.client.CustomerServiceTools;

// Les agents sont des ChatClient construits ici. Chacun reçoit SES outils : l'agent client ne connaît même pas
// l'existence des outils d'administration.
// @EnableAsync : indexation sémantique en arrière-plan (l'admin n'attend pas le modèle d'embeddings)
@Configuration
@EnableAsync
@EnableConfigurationProperties(AgentProperties.class)
class AgentConfig {

	// Mémoire : les N derniers messages de chaque conversation, conservés en base (table spring_ai_chat_memory)
	@Bean
	ChatMemory agentChatMemory(ChatMemoryRepository repository, AgentProperties properties) {
		return MessageWindowChatMemory.builder()
			.chatMemoryRepository(repository)
			.maxMessages(properties.memoryMessages())
			.build();
	}

	@Bean
	ChatClient clientAgent(ChatModel chatModel, ChatMemory chatMemory, ClientTools tools,
			CustomerServiceTools customerServiceTools, AgentProperties properties,
			ToolRunner runner, JsonMapper jsonMapper, ObjectProvider<ObservationRegistry> observationRegistry,
			@Value("classpath:agent/client-system-prompt.txt") Resource systemPrompt) {
		return ChatClient
			.builder(chatModel, observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP), null, null,
					toolLoop(properties, runner, jsonMapper))
			.defaultSystem(systemPrompt)
			.defaultToolCallbacks(guarded(runner, jsonMapper, tools, customerServiceTools))
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
			.build();
	}

	// Agent ADMIN : un autre ChatClient, d'autres outils, un autre prompt ; même boucle et mêmes garde-fous
	@Bean
	ChatClient adminAgent(ChatModel chatModel, ChatMemory chatMemory, AdminTools tools, AgentProperties properties,
			ToolRunner runner, JsonMapper jsonMapper, ObjectProvider<ObservationRegistry> observationRegistry,
			@Value("classpath:agent/admin-system-prompt.txt") Resource systemPrompt) {
		return ChatClient
			.builder(chatModel, observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP), null, null,
					toolLoop(properties, runner, jsonMapper))
			.defaultSystem(systemPrompt)
			.defaultToolCallbacks(guarded(runner, jsonMapper, tools))
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
			.build();
	}

	// Outils @Tool de ces objets, chacun enveloppé par GuardedToolCallback (arguments illisibles → refus, pas d'échec)
	static ToolCallback[] guarded(ToolRunner runner, JsonMapper jsonMapper, Object... toolObjects) {
		return Arrays.stream(MethodToolCallbackProvider.builder().toolObjects(toolObjects).build().getToolCallbacks())
			.map((callback) -> new GuardedToolCallback(callback, runner, jsonMapper))
			.toArray(ToolCallback[]::new);
	}

	// La boucle de l'agent (ToolCallingAdvisor) avec ses garde-fous :
	// - limites d'appels : au-delà, Spring AI arrête la boucle (réponse « toolCallLimitExceeded », voir AgentChatService) ;
	// - outil absent de CET agent : au lieu d'une exception qui ferait échouer le tour, le resolver de secours
	//   renvoie un refus journalisé (seuls les outils de l'agent restent exécutables)
	static ToolCallingAdvisor.Builder<?> toolLoop(AgentProperties properties, ToolRunner runner, JsonMapper jsonMapper) {
		return ToolCallingAdvisor.builder()
			.toolCallingManager(DefaultToolCallingManager.builder()
				.maxTotalToolCalls(properties.maxToolCalls())
				.maxCallsPerTool(properties.maxCallsPerTool())
				.resolutionFallbackEnabled(true)
				.toolCallbackResolver((toolName) -> new RefusedToolCallback(toolName, runner, jsonMapper))
				.build());
	}

}
