package com.boutique.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

// Test unitaire du gestionnaire d'erreurs : l'envoi multipart réel (limite de Tomcat) ne passe pas par MockMvc
class GlobalExceptionHandlerTest {

	@Test
	void tooLargeUploadGetsAFrenchProblemDetail() {
		ResponseEntity<Object> response = new GlobalExceptionHandler().handleMaxUploadSizeExceededException(
				new MaxUploadSizeExceededException(5 * 1024 * 1024), new HttpHeaders(), HttpStatusCode.valueOf(413),
				new ServletWebRequest(new MockHttpServletRequest()));

		assertThat(response.getStatusCode().value()).isEqualTo(413);
		ProblemDetail problem = (ProblemDetail) response.getBody();
		assertThat(problem.getTitle()).isEqualTo("Fichier trop volumineux");
		assertThat(problem.getDetail()).isEqualTo("Fichier trop volumineux : 5 Mo maximum par photo.");
	}

}
