package com.boutique.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import reactor.core.publisher.Flux;

// Faux modèle pour les tests : il joue un scénario écrit à l'avance (« appelle tel outil », puis « réponds tel texte »)
// et enregistre chaque requête reçue. Il peut aussi jouer un modèle MANIPULÉ (outil interdit, mensonge, prix inventé) :
// on vérifie alors que ce sont les garde-fous du code, et pas la bonne volonté du modèle, qui protègent la boutique.
public class ScriptedChatModel implements ChatModel {

	private final Queue<AssistantMessage> script = new ConcurrentLinkedQueue<>();

	private final List<Prompt> prompts = new CopyOnWriteArrayList<>();

	private final AtomicInteger toolCallIds = new AtomicInteger();

	private volatile AssistantMessage whenScriptIsOver = text("Fin du scénario.");

	private volatile String[] loopingTool;

	public ScriptedChatModel callsTool(String name, String jsonArguments) {
		script.add(toolCall(name, jsonArguments));
		return this;
	}

	public ScriptedChatModel answers(String text) {
		script.add(text(text));
		return this;
	}

	// Modèle qui boucle : il redemande le même outil indéfiniment (seule la limite d'appels l'arrête)
	public ScriptedChatModel keepsCallingTool(String name, String jsonArguments) {
		script.clear();
		script.add(toolCall(name, jsonArguments));
		loopingTool = new String[] { name, jsonArguments };
		return this;
	}

	public void reset() {
		script.clear();
		prompts.clear();
		loopingTool = null;
		whenScriptIsOver = text("Fin du scénario.");
	}

	public List<Prompt> prompts() {
		return List.copyOf(prompts);
	}

	// Texte renvoyé par un outil au modèle (ce que le modèle a réellement lu)
	public List<String> toolResults(String toolName) {
		return prompts.stream()
			.flatMap((prompt) -> prompt.getInstructions().stream())
			.filter(ToolResponseMessage.class::isInstance)
			.flatMap((message) -> ((ToolResponseMessage) message).getResponses().stream())
			.filter((response) -> response.name().equals(toolName))
			.map(ToolResponseMessage.ToolResponse::responseData)
			.distinct()
			.toList();
	}

	// Noms des outils proposés au modèle lors de la première requête
	public List<String> offeredTools() {
		return ((ToolCallingChatOptions) prompts.getFirst().getOptions()).getToolCallbacks()
			.stream()
			.map(ToolCallback::getToolDefinition)
			.map((definition) -> definition.name())
			.sorted()
			.toList();
	}

	@Override
	public ChatOptions getOptions() {
		// ToolCallingChatOptions : sans elles, le ChatClient ne transmet pas les outils au modèle
		return ToolCallingChatOptions.builder().build();
	}

	@Override
	public ChatResponse call(Prompt prompt) {
		return new ChatResponse(List.of(new Generation(next(prompt))));
	}

	@Override
	public Flux<ChatResponse> stream(Prompt prompt) {
		AssistantMessage message = next(prompt);
		if (message.hasToolCalls()) {
			return Flux.just(new ChatResponse(List.of(new Generation(message))));
		}
		// Le texte arrive par morceaux, comme avec un vrai modèle
		return Flux.fromIterable(Arrays.asList(message.getText().split("(?<= )")))
			.map((chunk) -> new ChatResponse(List.of(new Generation(new AssistantMessage(chunk)))));
	}

	private AssistantMessage next(Prompt prompt) {
		prompts.add(prompt);
		AssistantMessage message = script.poll();
		if (message != null) {
			return message;
		}
		String[] looping = loopingTool;
		return (looping != null) ? toolCall(looping[0], looping[1]) : whenScriptIsOver;
	}

	private AssistantMessage toolCall(String name, String jsonArguments) {
		return AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(new AssistantMessage.ToolCall("call-" + toolCallIds.incrementAndGet(), "function", name,
					jsonArguments)))
			.build();
	}

	private static AssistantMessage text(String text) {
		return new AssistantMessage(text);
	}

}
