package com.boutique.agent;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.boutique.agent.ToolCallLog.Outcome;
import com.boutique.common.BadRequestException;
import com.boutique.common.ConflictException;
import com.boutique.common.ForbiddenException;
import com.boutique.common.NotFoundException;

// Exécute un outil en le journalisant. Un refus métier (introuvable, stock insuffisant…) est renvoyé au modèle sous
// forme d'un message d'erreur lisible, pour qu'il l'explique au client ; une panne inattendue reste générique.
@Component
public class ToolRunner {

	private static final Logger log = LoggerFactory.getLogger(ToolRunner.class);

	private final ToolCallLogRepository repository;

	public ToolRunner(ToolCallLogRepository repository) {
		this.repository = repository;
	}

	public Object run(TurnContext turn, String tool, String arguments, Supplier<?> action) {
		long start = System.nanoTime();
		try {
			Object result = action.get();
			save(turn, tool, arguments, Outcome.OK, null, start);
			return result;
		}
		catch (NotFoundException | ConflictException | BadRequestException | ForbiddenException ex) {
			save(turn, tool, arguments, Outcome.REFUSED, ex.getMessage(), start);
			return Map.of("erreur", ex.getMessage());
		}
		catch (RuntimeException ex) {
			log.error("Outil {} en échec (utilisateur {})", tool, turn.userId(), ex);
			save(turn, tool, arguments, Outcome.ERROR, ex.getClass().getSimpleName(), start);
			return Map.of("erreur", "Information momentanément indisponible.");
		}
	}

	// Refus décidé par l'outil lui-même (garde-fou), journalisé comme tel
	public Object refuse(TurnContext turn, String tool, String arguments, String reason) {
		save(turn, tool, arguments, Outcome.REFUSED, reason, System.nanoTime());
		return Map.of("erreur", reason);
	}

	private void save(TurnContext turn, String tool, String arguments, Outcome outcome, String detail, long start) {
		int durationMs = (int) ((System.nanoTime() - start) / 1_000_000);
		repository.save(new ToolCallLog(turn.userId(), turn.agent(), tool, arguments, outcome, detail, durationMs));
	}

}
