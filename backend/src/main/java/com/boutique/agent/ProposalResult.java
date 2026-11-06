package com.boutique.agent;

import java.util.List;

// Compte rendu après confirmation : chaque étape a réussi ou échoué (les autres continuent)
public record ProposalResult(List<StepResult> steps) {

	public record StepResult(String summary, boolean success, String message) {
	}

	public String describe() {
		StringBuilder text = new StringBuilder();
		for (StepResult step : steps) {
			text.append(step.success() ? "✔ " : "✘ ").append(step.summary()).append(" : ").append(step.message()).append('\n');
		}
		return text.toString().strip();
	}

}
