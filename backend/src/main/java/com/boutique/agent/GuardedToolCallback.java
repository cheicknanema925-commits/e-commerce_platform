package com.boutique.agent;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Enveloppe chaque outil : un petit modèle écrit parfois « "orderId": "ma dernière commande" » là où un nombre est
// attendu. Spring AI renverrait au modèle le message technique de Jackson, en anglais, sans trace dans notre journal
// (vu à l'évaluation : le modèle a répondu « erreur de paramètre » à l'admin). Ici : un refus clair, journalisé.
final class GuardedToolCallback implements ToolCallback {

	private final ToolCallback delegate;

	private final ToolRunner runner;

	private final JsonMapper jsonMapper;

	GuardedToolCallback(ToolCallback delegate, ToolRunner runner, JsonMapper jsonMapper) {
		this.delegate = delegate;
		this.runner = runner;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		return delegate.call(toolInput);
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		try {
			return delegate.call(toolInput, toolContext);
		}
		catch (ToolExecutionException ex) {
			// Seule la lecture des arguments (cause Jackson) est rattrapée ; toute autre erreur suit son chemin normal
			if (!(ex.getCause() instanceof JacksonException)) {
				throw ex;
			}
			Object refusal = runner.refuse(TurnContext.from(toolContext), getToolDefinition().name(), toolInput,
					"Arguments illisibles : identifiants, quantités, remises et prix s'écrivent en chiffres (2, pas « deux »). "
							+ "Corrige l'appel ou pose une question au client.");
			return jsonMapper.writeValueAsString(refusal);
		}
	}

}
