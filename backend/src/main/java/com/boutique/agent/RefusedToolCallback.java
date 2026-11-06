package com.boutique.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import tools.jackson.databind.json.JsonMapper;

// Le modèle a demandé un outil que CET agent n'a pas : nom inventé (« addToCart »), ou outil d'un autre agent
// (« applyPriceChange » demandé à l'assistant client, par exemple après une injection dans un avis).
// Rien n'est exécuté : le refus est journalisé et renvoyé au modèle, qui peut s'excuser au lieu de faire échouer le tour.
final class RefusedToolCallback implements ToolCallback {

	private final ToolDefinition definition;

	private final ToolRunner runner;

	private final JsonMapper jsonMapper;

	RefusedToolCallback(String toolName, ToolRunner runner, JsonMapper jsonMapper) {
		this.definition = ToolDefinition.builder().name(toolName).description("Outil indisponible").inputSchema("{}").build();
		this.runner = runner;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return definition;
	}

	@Override
	public String call(String toolInput) {
		throw new IllegalStateException("Outil appelé sans contexte de tour");
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		Object refusal = runner.refuse(TurnContext.from(toolContext), definition.name(), toolInput,
				"Outil inconnu ou non autorisé pour cet assistant : " + definition.name()
						+ ". Utilise uniquement les outils qui te sont fournis.");
		return jsonMapper.writeValueAsString(refusal);
	}

}
