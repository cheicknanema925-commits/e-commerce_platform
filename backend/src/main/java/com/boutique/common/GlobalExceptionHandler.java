package com.boutique.common;

import java.util.Comparator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Toutes les erreurs de tous les contrôleurs arrivent ici et repartent au format ProblemDetail (RFC 9457).
// La classe parente gère déjà les erreurs de Spring MVC (JSON illisible, méthode non supportée, URL inconnue...).
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	ProblemDetail handleBadRequest(BadRequestException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	ProblemDetail handleUnauthorized(UnauthorizedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Non authentifié", ex.getMessage());
	}

	// @PreAuthorize refusé dans un contrôleur : sans ce gestionnaire, le filet Exception ci-dessous renverrait 500
	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		return problem(HttpStatus.FORBIDDEN, "Accès refusé", "Vous n'avez pas les droits nécessaires.");
	}

	// Contrainte de la base violée (ex. supprimer un produit déjà commandé) : le message SQL reste dans les logs
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		log.warn("Contrainte d'intégrité violée : {}", ex.getMostSpecificCause().getMessage());
		return problem(HttpStatus.CONFLICT, "Conflit",
				"Opération impossible : cette ressource est liée à d'autres données.");
	}

	// Verrouillage optimiste : quelqu'un a modifié la même donnée au même moment (ex. le dernier article acheté
	// par deux clients) ; le client peut simplement réessayer avec des données à jour
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
		return problem(HttpStatus.CONFLICT, "Conflit",
				"Ces données viennent d'être modifiées par une autre opération. Réessayez.");
	}

	// Tri ou filtre sur un champ qui n'existe pas (ex. ?sort=motdepasse)
	@ExceptionHandler(PropertyReferenceException.class)
	ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Paramètre invalide", "Propriété inconnue : " + ex.getPropertyName());
	}

	// Filet de sécurité : le client reçoit seulement une référence, le détail technique va dans les logs
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		String errorId = UUID.randomUUID().toString();
		log.error("Erreur interne {}", errorId, ex);
		ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
				"Une erreur inattendue est survenue. Référence : " + errorId);
		problem.setProperty("errorId", errorId);
		return problem;
	}

	// @Valid a échoué : on ajoute la liste des champs invalides à la réponse 400
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ex.getBody();
		problem.setTitle("Données invalides");
		problem.setDetail("Un ou plusieurs champs sont invalides.");
		problem.setProperty("errors",
				ex.getBindingResult()
					.getFieldErrors()
					.stream()
					.map((error) -> new InvalidField(error.getField(), error.getDefaultMessage()))
					.sorted(Comparator.comparing(InvalidField::field))
					.toList());
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

	record InvalidField(String field, String message) {
	}

}
